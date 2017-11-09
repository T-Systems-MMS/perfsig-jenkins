/*
 * Copyright (c) 2014 T-Systems Multimedia Solutions GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tsystems.mms.apm.performancesignature.dynatrace.rest.json;

import com.squareup.okhttp.*;
import com.squareup.okhttp.internal.http.HttpMethod;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;
import com.squareup.okhttp.logging.HttpLoggingInterceptor.Level;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.json.auth.Authentication;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.json.auth.HttpBasicAuth;
import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiClient {
    /**
     * The datetime format to be used when <code>lenientDatetimeFormat</code> is enabled.
     */
    public final static String API_SUFFIX = "/api/v2";
    private static final String LENIENT_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private final Map<String, String> defaultHeaderMap = new HashMap<>();
    private String basePath = "https://localhost/api/v2";
    private boolean lenientOnJson = false;
    private boolean debugging = false;
    private String tempFolderPath = null;

    private Map<String, Authentication> authentications;

    private DateFormat dateFormat;
    private DateFormat datetimeFormat;
    private boolean lenientDatetimeFormat;
    private int dateLength;

    private boolean verifyingSsl;

    private OkHttpClient httpClient;
    private JSON json;

    private HttpLoggingInterceptor loggingInterceptor;

    /*
     * Constructor for ApiClient
     */
    public ApiClient() {
        httpClient = new OkHttpClient();

        verifyingSsl = true;

        json = new JSON(this);

        /*
         * Use RFC3339 format for date and datetime.
         * See http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14
         */
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // Always use UTC as the default time zone when dealing with date (without time).
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        initDatetimeFormat();

        // Be lenient on datetime formats when parsing datetime from string.
        // See <code>parseDatetime</code>.
        this.lenientDatetimeFormat = true;

        // Setup authentications (key: authentication name, value: authentication).
        authentications = new HashMap<>();
        authentications.put("basicAuth", new HttpBasicAuth());
        // Prevent the authentications from being modified.
        authentications = Collections.unmodifiableMap(authentications);
    }

    /**
     * Get base path
     *
     * @return Baes path
     */
    public String getBasePath() {
        return basePath;
    }

    /**
     * Set base path
     *
     * @param basePath Base path of the URL (e.g https://localhost/api/v2
     * @return An instance of OkHttpClient
     */
    public ApiClient setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    /**
     * Get HTTP client
     *
     * @return An instance of OkHttpClient
     */
    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Set HTTP client
     *
     * @param httpClient An instance of OkHttpClient
     * @return Api Client
     */
    public ApiClient setHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Get JSON
     *
     * @return JSON object
     */
    public JSON getJSON() {
        return json;
    }

    /**
     * Set JSON
     *
     * @param json JSON object
     * @return Api client
     */
    public ApiClient setJSON(JSON json) {
        this.json = json;
        return this;
    }

    /**
     * True if isVerifyingSsl flag is on
     *
     * @return True if isVerifySsl flag is on
     */
    public boolean isVerifyingSsl() {
        return verifyingSsl;
    }

    /**
     * Configure whether to verify certificate and hostname when making https requests.
     * Default to true.
     * NOTE: Do NOT set to false in production code, otherwise you would face multiple types of cryptographic attacks.
     *
     * @param verifyingSsl True to verify TLS/SSL connection
     * @return ApiClient
     */
    public ApiClient setVerifyingSsl(boolean verifyingSsl) {
        this.verifyingSsl = verifyingSsl;
        applySslSettings();
        return this;
    }

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public ApiClient setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        this.dateLength = this.dateFormat.format(new Date()).length();
        return this;
    }

    public DateFormat getDatetimeFormat() {
        return datetimeFormat;
    }

    public ApiClient setDatetimeFormat(DateFormat datetimeFormat) {
        this.datetimeFormat = datetimeFormat;
        return this;
    }

    /**
     * Whether to allow various ISO 8601 datetime formats when parsing a datetime string.
     *
     * @return True if lenientDatetimeFormat flag is set to true
     * @see #parseDatetime(String)
     */
    public boolean isLenientDatetimeFormat() {
        return lenientDatetimeFormat;
    }

    public ApiClient setLenientDatetimeFormat(boolean lenientDatetimeFormat) {
        this.lenientDatetimeFormat = lenientDatetimeFormat;
        return this;
    }

    /**
     * Parse the given date string into Date object.
     * The default <code>dateFormat</code> supports these ISO 8601 date formats:
     * 2015-08-16
     * 2015-8-16
     *
     * @param str String to be parsed
     * @return Date
     */
    public Date parseDate(String str) {
        if (str == null)
            return null;
        try {
            return dateFormat.parse(str);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parse the given datetime string into Date object.
     * When lenientDatetimeFormat is enabled, the following ISO 8601 datetime formats are supported:
     * 2015-08-16T08:20:05Z
     * 2015-8-16T8:20:05Z
     * 2015-08-16T08:20:05+00:00
     * 2015-08-16T08:20:05+0000
     * 2015-08-16T08:20:05.376Z
     * 2015-08-16T08:20:05.376+00:00
     * 2015-08-16T08:20:05.376+00
     * Note: The 3-digit milli-seconds is optional. Time zone is required and can be in one of
     * these formats:
     * Z (same with +0000)
     * +08:00 (same with +0800)
     * -02 (same with -0200)
     * -0200
     *
     * @param str Date time string to be parsed
     * @return Date representation of the string
     * @see <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>
     */
    public Date parseDatetime(String str) {
        if (str == null)
            return null;

        DateFormat format;
        if (lenientDatetimeFormat) {
            /*
             * When lenientDatetimeFormat is enabled, normalize the date string
             * into <code>LENIENT_DATETIME_FORMAT</code> to support various formats
             * defined by ISO 8601.
             */
            // normalize time zone
            //   trailing "Z": 2015-08-16T08:20:05Z => 2015-08-16T08:20:05+0000
            str = str.replaceAll("[zZ]\\z", "+0000");
            //   remove colon in time zone: 2015-08-16T08:20:05+00:00 => 2015-08-16T08:20:05+0000
            str = str.replaceAll("([+-]\\d{2}):(\\d{2})\\z", "$1$2");
            //   expand time zone: 2015-08-16T08:20:05+00 => 2015-08-16T08:20:05+0000
            str = str.replaceAll("([+-]\\d{2})\\z", "$100");
            // add milliseconds when missing
            //   2015-08-16T08:20:05+0000 => 2015-08-16T08:20:05.000+0000
            str = str.replaceAll("(:\\d{1,2})([+-]\\d{4})\\z", "$1.000$2");
            format = new SimpleDateFormat(LENIENT_DATETIME_FORMAT);
        } else {
            format = this.datetimeFormat;
        }

        try {
            return format.parse(str);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Parse date or date time in string format into Date object.
     *
     * @param str Date time string to be parsed
     * @return Date representation of the string
     */
    public Date parseDateOrDatetime(String str) {
        if (str == null)
            return null;
        else if (str.length() <= dateLength)
            return parseDate(str);
        else
            return parseDatetime(str);
    }

    /**
     * Format the given Date object into string (Date format).
     *
     * @param date Date object
     * @return Formatted date in string representation
     */
    public String formatDate(Date date) {
        return dateFormat.format(date);
    }

    /**
     * Format the given Date object into string (Datetime format).
     *
     * @param date Date object
     * @return Formatted datetime in string representation
     */
    public String formatDatetime(Date date) {
        return datetimeFormat.format(date);
    }

    /**
     * Get authentications (key: authentication name, value: authentication).
     *
     * @return Map of authentication objects
     */
    public Map<String, Authentication> getAuthentications() {
        return authentications;
    }

    /**
     * Get authentication for the given name.
     *
     * @param authName The authentication name
     * @return The authentication, null if not found
     */
    public Authentication getAuthentication(String authName) {
        return authentications.get(authName);
    }

    /**
     * Helper method to set username for the first HTTP basic authentication.
     *
     * @param username Username
     */
    public void setUsername(String username) {
        for (Authentication auth : authentications.values()) {
            if (auth instanceof HttpBasicAuth) {
                ((HttpBasicAuth) auth).setUsername(username);
                return;
            }
        }
        throw new RuntimeException("No HTTP basic authentication configured!");
    }

    /**
     * Helper method to set password for the first HTTP basic authentication.
     *
     * @param password Password
     */
    public void setPassword(String password) {
        for (Authentication auth : authentications.values()) {
            if (auth instanceof HttpBasicAuth) {
                ((HttpBasicAuth) auth).setPassword(password);
                return;
            }
        }
        throw new RuntimeException("No HTTP basic authentication configured!");
    }

    /**
     * Set the User-Agent header's value (by adding to the default header map).
     *
     * @param userAgent HTTP request's user agent
     * @return ApiClient
     */
    public ApiClient setUserAgent(String userAgent) {
        addDefaultHeader("User-Agent", userAgent);
        return this;
    }

    /**
     * Add a default header.
     *
     * @param key   The header's key
     * @param value The header's value
     * @return ApiClient
     */
    public ApiClient addDefaultHeader(String key, String value) {
        defaultHeaderMap.put(key, value);
        return this;
    }

    /**
     * @return True if lenientOnJson is enabled, false otherwise.
     * @see <a href="https://google-gson.googlecode.com/svn/trunk/gson/docs/javadocs/com/google/gson/stream/JsonReader.html#setLenient(boolean)">setLenient</a>
     */
    public boolean isLenientOnJson() {
        return lenientOnJson;
    }

    /**
     * Set LenientOnJson
     *
     * @param lenient True to enable lenientOnJson
     * @return ApiClient
     */
    public ApiClient setLenientOnJson(boolean lenient) {
        this.lenientOnJson = lenient;
        return this;
    }

    /**
     * Check that whether debugging is enabled for this API client.
     *
     * @return True if debugging is enabled, false otherwise.
     */
    public boolean isDebugging() {
        return debugging;
    }

    /**
     * Enable/disable debugging for this API client.
     *
     * @param debugging To enable (true) or disable (false) debugging
     * @return ApiClient
     */
    public ApiClient setDebugging(boolean debugging) {
        if (debugging != this.debugging) {
            if (debugging) {
                loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(Level.BODY);
                httpClient.interceptors().add(loggingInterceptor);
            } else {
                httpClient.interceptors().remove(loggingInterceptor);
                loggingInterceptor = null;
            }
        }
        this.debugging = debugging;
        return this;
    }

    /**
     * The path of temporary folder used to store downloaded files from endpoints
     * with file response. The default value is <code>null</code>, i.e. using
     * the system's default tempopary folder.
     *
     * @return Temporary folder path
     * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/io/File.html#createTempFile">createTempFile</a>
     */
    public String getTempFolderPath() {
        return tempFolderPath;
    }

    /**
     * Set the tempoaray folder path (for downloading files)
     *
     * @param tempFolderPath Temporary folder path
     * @return ApiClient
     */
    public ApiClient setTempFolderPath(String tempFolderPath) {
        this.tempFolderPath = tempFolderPath;
        return this;
    }

    /**
     * Get connection timeout (in milliseconds).
     *
     * @return Timeout in milliseconds
     */
    public int getConnectTimeout() {
        return httpClient.getConnectTimeout();
    }

    /**
     * Sets the connect timeout (in milliseconds).
     * A value of 0 means no timeout, otherwise values must be between 1 and
     *
     * @param connectionTimeout connection timeout in milliseconds
     * @return Api client
     */
    public ApiClient setConnectTimeout(int connectionTimeout) {
        httpClient.setConnectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * Format the given parameter object into string.
     *
     * @param param Parameter
     * @return String representation of the parameter
     */
    public String parameterToString(Object param) {
        if (param == null) {
            return "";
        } else if (param instanceof Date) {
            return formatDatetime((Date) param);
        } else if (param instanceof Collection) {
            StringBuilder b = new StringBuilder();
            for (Object o : (Collection) param) {
                if (b.length() > 0) {
                    b.append(",");
                }
                b.append(String.valueOf(o));
            }
            return b.toString();
        } else {
            return String.valueOf(param);
        }
    }

    /**
     * Format to {@code Pair} objects.
     *
     * @param collectionFormat collection format (e.g. csv, tsv)
     * @param name             Name
     * @param value            Value
     * @return A list of Pair objects
     */
    public List<Pair> parameterToPairs(String collectionFormat, String name, Object value) {
        List<Pair> params = new ArrayList<>();

        // preconditions
        if (name == null || name.isEmpty() || value == null) return params;

        Collection valueCollection;
        if (value instanceof Collection) {
            valueCollection = (Collection) value;
        } else {
            params.add(new Pair(name, parameterToString(value)));
            return params;
        }

        if (valueCollection.isEmpty()) {
            return params;
        }

        // get the collection format
        collectionFormat = (collectionFormat == null || collectionFormat.isEmpty() ? "csv" : collectionFormat); // default: csv

        // create the params based on the collection format
        if (collectionFormat.equals("multi")) {
            for (Object item : valueCollection) {
                params.add(new Pair(name, parameterToString(item)));
            }

            return params;
        }

        String delimiter;
        switch (collectionFormat) {
            case "csv":
                delimiter = ",";
                break;
            case "ssv":
                delimiter = " ";
                break;
            case "tsv":
                delimiter = "\t";
                break;
            case "pipes":
                delimiter = "|";
                break;
            default:
                delimiter = ",";
        }

        StringBuilder sb = new StringBuilder();
        for (Object item : valueCollection) {
            sb.append(delimiter);
            sb.append(parameterToString(item));
        }

        params.add(new Pair(name, sb.substring(1)));

        return params;
    }

    /**
     * Sanitize filename by removing path.
     * e.g. ../../sun.gif becomes sun.gif
     *
     * @param filename The filename to be sanitized
     * @return The sanitized filename
     */
    public String sanitizeFilename(String filename) {
        return filename.replaceAll(".*[/\\\\]", "");
    }

    /**
     * Check if the given MIME is a JSON MIME.
     * JSON MIME examples:
     * application/json
     * application/json; charset=UTF8
     * APPLICATION/JSON
     * application/vnd.company+json
     *
     * @param mime MIME (Multipurpose Internet Mail Extensions)
     * @return True if the given MIME is JSON, false otherwise.
     */
    public boolean isJsonMime(String mime) {
        String jsonMime = "(?i)^(application/json|[^;/ \t]+/[^;/ \t]+[+]json)[ \t]*(;.*)?$";
        return mime != null && (mime.matches(jsonMime) || mime.equalsIgnoreCase("application/json-patch+json"));
    }

    /**
     * Check if the given MIME is a XML MIME.
     * XML MIME examples:
     * text/xml
     *
     * @param mime MIME (Multipurpose Internet Mail Extensions)
     * @return True if the given MIME is XML, false otherwise.
     */
    public boolean isXmlMime(String mime) {
        String xmlMime = "(?i)^(text/xml)[ \t]*(;.*)?$";
        return mime != null && (mime.matches(xmlMime));
    }

    /**
     * Select the Accept header's value from the given accepts array:
     * if JSON exists in the given array, use it;
     * otherwise use all of them (joining into a string)
     *
     * @param accepts The accepts array to select from
     * @return The Accept header to use. If the given array is empty,
     * null will be returned (not to set the Accept header explicitly).
     */
    public String selectHeaderAccept(String[] accepts) {
        if (accepts.length == 0) {
            return null;
        }
        for (String accept : accepts) {
            if (isJsonMime(accept)) {
                return accept;
            }
        }
        return StringUtils.join(accepts, ",");
    }

    /**
     * Select the Content-Type header's value from the given array:
     * if JSON exists in the given array, use it;
     * otherwise use the first one of the array.
     *
     * @param contentTypes The Content-Type array to select from
     * @return The Content-Type header to use. If the given array is empty,
     * JSON will be used.
     */
    public String selectHeaderContentType(String[] contentTypes) {
        if (contentTypes.length == 0) {
            return "application/json";
        }
        for (String contentType : contentTypes) {
            if (isJsonMime(contentType)) {
                return contentType;
            }
        }
        return contentTypes[0];
    }

    /**
     * Escape the given string to be used as URL query value.
     *
     * @param str String to be escaped
     * @return Escaped string
     */
    public String escapeString(String str) {
        try {
            return URLEncoder.encode(str, "utf8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * Unescape the given string coming from a query parameter.
     *
     * @param str String to be unescaped
     * @return unescaped string
     */
    public String unescapeString(String str) {
        try {
            String decoded = URLDecoder.decode(str, "utf8");
            return URLDecoder.decode(decoded, "utf8");
        } catch (UnsupportedEncodingException e) {
            return str;
        }
    }

    /**
     * Deserialize response body to Java object, according to the return type and
     * the Content-Type response header.
     *
     * @param <T>        Type
     * @param response   HTTP response
     * @param returnType The type of the Java object
     * @return The deserialized Java object
     * @throws ApiException If fail to deserialize response body, i.e. cannot read response body
     *                      or the Content-Type of the response is not supported.
     */
    @SuppressWarnings("unchecked")
    public <T> T deserialize(Response response, Type returnType) throws ApiException {
        if (response == null || returnType == null) {
            return null;
        }

        if ("byte[]".equals(returnType.toString())) {
            // Handle binary response (byte array).
            try {
                return (T) response.body().bytes();
            } catch (IOException e) {
                throw new ApiException(e);
            }
        } else if (returnType.equals(File.class)) {
            // Handle file downloading.
            return (T) downloadFileFromResponse(response);
        }

        String respBody;
        try {
            if (response.body() != null)
                respBody = response.body().string();
            else
                respBody = null;
        } catch (IOException e) {
            throw new ApiException(e);
        }

        if (respBody == null || "".equals(respBody)) {
            return null;
        }

        String contentType = response.headers().get("Content-Type");
        if (contentType == null) {
            // ensuring a default content type
            contentType = "application/json";
        }
        if (isJsonMime(contentType)) {
            return json.deserialize(respBody, returnType);
        } else if (isXmlMime(contentType)) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance((Class) returnType);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                return (T) jaxbUnmarshaller.unmarshal(new StringReader(respBody));
            } catch (JAXBException e) {
                throw new ApiException("could not parse xml" + e.getMessage());
            }
        } else if (returnType.equals(String.class)) {
            // Expecting string, return the raw response body.
            return (T) respBody;
        } else {
            throw new ApiException(
                    "Content type \"" + contentType + "\" is not supported for type: " + returnType,
                    response.code(),
                    response.headers().toMultimap(),
                    respBody);
        }
    }

    /**
     * Serialize the given Java object into request body according to the object's
     * class and the request Content-Type.
     *
     * @param obj         The Java object
     * @param contentType The request Content-Type
     * @return The serialized request body
     * @throws ApiException If fail to serialize the given object
     */
    public RequestBody serialize(Object obj, String contentType) throws ApiException {
        if (obj instanceof byte[]) {
            // Binary (byte array) body parameter support.
            return RequestBody.create(MediaType.parse(contentType), (byte[]) obj);
        } else if (obj instanceof File) {
            // File body parameter support.
            return RequestBody.create(MediaType.parse(contentType), (File) obj);
        } else if (isJsonMime(contentType)) {
            String content;
            if (obj != null) {
                content = json.serialize(obj);
            } else {
                content = null;
            }
            return RequestBody.create(MediaType.parse(contentType), content);
        } else {
            throw new ApiException("Content type \"" + contentType + "\" is not supported");
        }
    }

    /**
     * Download file from the given response.
     *
     * @param response An instance of the Response object
     * @return Downloaded file
     * @throws ApiException If fail to read file content from response and write to disk
     */
    public File downloadFileFromResponse(Response response) throws ApiException {
        try {
            File file = prepareDownloadFile(response);
            BufferedSink sink = Okio.buffer(Okio.sink(file));
            sink.writeAll(response.body().source());
            sink.close();
            return file;
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    /**
     * Prepare file for download
     *
     * @param response An instance of the Response object
     * @return Prepared file for the download
     * @throws IOException If fail to prepare file for download
     */
    public File prepareDownloadFile(Response response) throws IOException {
        String filename = null;
        String contentDisposition = response.header("Content-Disposition");
        if (contentDisposition != null && !"".equals(contentDisposition)) {
            // Get filename from the Content-Disposition header.
            Pattern pattern = Pattern.compile("filename=['\"]?([^'\"\\s]+)['\"]?");
            Matcher matcher = pattern.matcher(contentDisposition);
            if (matcher.find()) {
                filename = sanitizeFilename(matcher.group(1));
            }
        }

        String prefix;
        String suffix = null;
        if (filename == null) {
            prefix = "download-";
            suffix = "";
        } else {
            int pos = filename.lastIndexOf(".");
            if (pos == -1) {
                prefix = filename + "-";
            } else {
                prefix = filename.substring(0, pos) + "-";
                suffix = filename.substring(pos);
            }
            // File.createTempFile requires the prefix to be at least three characters long
            if (prefix.length() < 3)
                prefix = "download-";
        }

        if (tempFolderPath == null)
            return File.createTempFile(prefix, suffix);
        else
            return File.createTempFile(prefix, suffix, new File(tempFolderPath));
    }

    /**
     * {@link #execute(Call, Type)}
     *
     * @param <T>  Type
     * @param call An instance of the Call object
     * @return ApiResponse&lt;T&gt;
     * @throws ApiException If fail to execute the call
     */
    public <T> ApiResponse<T> execute(Call call) throws ApiException {
        return execute(call, null);
    }

    /**
     * Execute HTTP call and deserialize the HTTP response body into the given return type.
     *
     * @param returnType The return type used to deserialize HTTP response body
     * @param <T>        The return type corresponding to (same with) returnType
     * @param call       Call
     * @return ApiResponse object containing response status, headers and
     * data, which is a Java object deserialized from response body and would be null
     * when returnType is null.
     * @throws ApiException If fail to execute the call
     */
    public <T> ApiResponse<T> execute(Call call, Type returnType) throws ApiException {
        try {
            Response response = call.execute();
            T data = handleResponse(response, returnType);
            return new ApiResponse<>(response.code(), response.headers().toMultimap(), data);
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }

    /**
     * Handle the given response, return the deserialized object when the response is successful.
     *
     * @param <T>        Type
     * @param response   Response
     * @param returnType Return type
     * @return Type
     * @throws ApiException If the response has a unsuccessful status code or
     *                      fail to deserialize the response body
     */
    public <T> T handleResponse(Response response, Type returnType) throws ApiException {
        if (response.isSuccessful()) {
            if (returnType == null || response.code() == 204) {
                // returning null if the returnType is not defined,
                // or the status code is 204 (No Content)
                if (response.body() != null) {
                    try {
                        response.body().close();
                    } catch (IOException e) {
                        throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
                    }
                }
                return null;
            } else {
                return deserialize(response, returnType);
            }
        } else {
            String respBody = null;
            if (response.body() != null) {
                try {
                    respBody = response.body().string();
                } catch (IOException e) {
                    throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
                }
            }
            throw new ApiException(response.message(), response.code(), response.headers().toMultimap(), respBody);
        }
    }

    /**
     * Build HTTP call with the given options.
     *
     * @param path         The sub-path of the HTTP URL
     * @param method       The request method, one of "GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH" and "DELETE"
     * @param queryParams  The query parameters
     * @param body         The request body object
     * @param headerParams The header parameters
     * @param formParams   The form parameters
     * @param authNames    The authentications to apply
     * @return The HTTP call
     * @throws ApiException If fail to serialize the request body object
     */
    public Call buildCall(String path, String method, List<Pair> queryParams, Object body, Map<String, String> headerParams, Map<String, Object> formParams, String[] authNames) throws ApiException {
        Request request = buildRequest(path, method, queryParams, body, headerParams, formParams, authNames);

        return httpClient.newCall(request);
    }

    /**
     * Build an HTTP request with the given options.
     *
     * @param path         The sub-path of the HTTP URL
     * @param method       The request method, one of "GET", "HEAD", "OPTIONS", "POST", "PUT", "PATCH" and "DELETE"
     * @param queryParams  The query parameters
     * @param body         The request body object
     * @param headerParams The header parameters
     * @param formParams   The form parameters
     * @param authNames    The authentications to apply
     * @return The HTTP request
     * @throws ApiException If fail to serialize the request body object
     */
    public Request buildRequest(String path, String method, List<Pair> queryParams, Object body, Map<String, String> headerParams, Map<String, Object> formParams, String[] authNames) throws ApiException {
        updateParamsForAuth(authNames, queryParams, headerParams);

        final String url = buildUrl(path, queryParams);
        final Request.Builder reqBuilder = new Request.Builder().url(url);
        processHeaderParams(headerParams, reqBuilder);

        String contentType = headerParams.get("Content-Type");
        // ensuring a default content type
        if (contentType == null) {
            contentType = "application/json";
        }

        RequestBody reqBody;
        if (!HttpMethod.permitsRequestBody(method)) {
            reqBody = null;
        } else if ("application/x-www-form-urlencoded".equals(contentType)) {
            reqBody = buildRequestBodyFormEncoding(formParams);
        } else if ("multipart/form-data".equals(contentType)) {
            reqBody = buildRequestBodyMultipart(formParams);
        } else if (body == null) {
            if ("DELETE".equals(method)) {
                // allow calling DELETE without sending a request body
                reqBody = null;
            } else {
                // use an empty request body (for POST, PUT and PATCH)
                reqBody = RequestBody.create(MediaType.parse(contentType), "");
            }
        } else {
            reqBody = serialize(body, contentType);
        }

        return reqBuilder.method(method, reqBody).build();
    }

    /**
     * Build full URL by concatenating base path, the given sub path and query parameters.
     *
     * @param path        The sub path
     * @param queryParams The query parameters
     * @return The full URL
     */
    public String buildUrl(String path, List<Pair> queryParams) {
        final StringBuilder url = new StringBuilder();
        url.append(basePath).append(path);

        if (queryParams != null && !queryParams.isEmpty()) {
            // support (constant) query string in `path`, e.g. "/posts?draft=1"
            String prefix = path.contains("?") ? "&" : "?";
            for (Pair param : queryParams) {
                if (param.getValue() != null) {
                    if (prefix != null) {
                        url.append(prefix);
                        prefix = null;
                    } else {
                        url.append("&");
                    }
                    String value = parameterToString(param.getValue());
                    url.append(escapeString(param.getName())).append("=").append(escapeString(value));
                }
            }
        }

        return url.toString();
    }

    /**
     * Set header parameters to the request builder, including default headers.
     *
     * @param headerParams Header parameters in the ofrm of Map
     * @param reqBuilder   Reqeust.Builder
     */
    public void processHeaderParams(Map<String, String> headerParams, Request.Builder reqBuilder) {
        for (Entry<String, String> param : headerParams.entrySet()) {
            reqBuilder.header(param.getKey(), parameterToString(param.getValue()));
        }
        for (Entry<String, String> header : defaultHeaderMap.entrySet()) {
            if (!headerParams.containsKey(header.getKey())) {
                reqBuilder.header(header.getKey(), parameterToString(header.getValue()));
            }
        }
    }

    /**
     * Update query and header parameters based on authentication settings.
     *
     * @param authNames    The authentications to apply
     * @param queryParams  List of query parameters
     * @param headerParams Map of header parameters
     */
    public void updateParamsForAuth(String[] authNames, List<Pair> queryParams, Map<String, String> headerParams) {
        for (String authName : authNames) {
            Authentication auth = authentications.get(authName);
            if (auth == null) throw new RuntimeException("Authentication undefined: " + authName);
            auth.applyToParams(queryParams, headerParams);
        }
    }

    /**
     * Build a form-encoding request body with the given form parameters.
     *
     * @param formParams Form parameters in the form of Map
     * @return RequestBody
     */
    public RequestBody buildRequestBodyFormEncoding(Map<String, Object> formParams) {
        FormEncodingBuilder formBuilder = new FormEncodingBuilder();
        for (Entry<String, Object> param : formParams.entrySet()) {
            formBuilder.add(param.getKey(), parameterToString(param.getValue()));
        }
        return formBuilder.build();
    }

    /**
     * Build a multipart (file uploading) request body with the given form parameters,
     * which could contain text fields and file fields.
     *
     * @param formParams Form parameters in the form of Map
     * @return RequestBody
     */
    public RequestBody buildRequestBodyMultipart(Map<String, Object> formParams) {
        MultipartBuilder mpBuilder = new MultipartBuilder().type(MultipartBuilder.FORM);
        for (Entry<String, Object> param : formParams.entrySet()) {
            if (param.getValue() instanceof File) {
                File file = (File) param.getValue();
                Headers partHeaders = Headers.of("Content-Disposition", "form-data; name=\"" + param.getKey() + "\"; filename=\"" + file.getName() + "\"");
                MediaType mediaType = MediaType.parse(guessContentTypeFromFile(file));
                mpBuilder.addPart(partHeaders, RequestBody.create(mediaType, file));
            } else {
                Headers partHeaders = Headers.of("Content-Disposition", "form-data; name=\"" + param.getKey() + "\"");
                mpBuilder.addPart(partHeaders, RequestBody.create(null, parameterToString(param.getValue())));
            }
        }
        return mpBuilder.build();
    }

    /**
     * Guess Content-Type header from the given file (defaults to "application/octet-stream").
     *
     * @param file The given file
     * @return The guessed Content-Type
     */
    public String guessContentTypeFromFile(File file) {
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        if (contentType == null) {
            return "application/octet-stream";
        } else {
            return contentType;
        }
    }

    /**
     * Initialize datetime format according to the current environment, e.g. Java 1.7 and Android.
     */
    private void initDatetimeFormat() {
        String formatWithTimeZone = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
        this.datetimeFormat = new SimpleDateFormat(formatWithTimeZone);
    }

    /**
     * Apply SSL related settings to httpClient according to the current values of
     * verifyingSsl and sslCaCert.
     */
    private void applySslSettings() {
        try {
            TrustManager[] trustManagers = null;
            HostnameVerifier hostnameVerifier = null;
            if (!verifyingSsl) {
                TrustManager trustAll = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                };
                trustManagers = new TrustManager[]{trustAll};
                hostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
            }

            if (trustManagers != null) {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, trustManagers, new SecureRandom());
                httpClient.setSslSocketFactory(sslContext.getSocketFactory());
            } else {
                httpClient.setSslSocketFactory(null);
            }
            httpClient.setHostnameVerifier(hostnameVerifier);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
