package com.zero.support.net.internal;


import com.zero.support.net.Headers;
import com.zero.support.net.HttpClient;
import com.zero.support.net.Interceptor;
import com.zero.support.net.MediaType;
import com.zero.support.net.Request;
import com.zero.support.net.RequestBody;
import com.zero.support.net.Response;
import com.zero.support.net.ResponseBody;

import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class CallServerInterceptor implements Interceptor {
    public static final int HTTP_CONTINUE = 100;
    static final String HEADER_CONTENT_TYPE = "Content-Type";
    HttpClient client;
    private boolean cancel;
    private Thread workThread;

    public CallServerInterceptor(HttpClient client) {
        this.client = client;
    }

    private static List<String> convertHeaders(Map<String, List<String>> responseHeaders) {
        List<String> headerList = new ArrayList<>(responseHeaders.size() * 2);
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            // HttpUrlConnection includes the status line as a header with a null key; omit it here
            // since it's not really a header and the rest of Volley assumes non-null keys.
            if (entry.getKey() != null) {
                for (String value : entry.getValue()) {
                    headerList.add(entry.getKey());
                    headerList.add(value.trim());
                }
            }
        }
        return headerList;
    }

    /**
     * Initializes an {@link InputStream} from the given {@link HttpURLConnection}.
     *
     * @param connection
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private static InputStream inputStreamFromConnection(HttpURLConnection connection) {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        return inputStream;
    }

    // NOTE: Any request headers added here (via setRequestProperty or addRequestProperty) should be
    // checked against the existing properties in the connection and not overridden if already set.
    @SuppressWarnings("deprecation")
    /* package */ static void setConnectionParametersForRequest(
            HttpURLConnection connection, Request request) throws IOException {

        String method = request.method();
        connection.setRequestMethod(method);
        addBodyIfExists(connection, request);
    }

    private static void addBodyIfExists(HttpURLConnection connection, Request request)
            throws IOException {
        RequestBody body = request.body();
        if (body != null) {
            addBody(connection, body);
        }
    }

    private static void addBody(HttpURLConnection connection, RequestBody requestBody)
            throws IOException {
        // Prepare output. There is no need to set Content-Length explicitly,
        // since this is mHandled by HttpURLConnection using the size of the prepared
        // output stream.
        connection.setDoOutput(true);
        // Set the content-type unless it was already set (by IntentRequest#getHeaders).
        if (!connection.getRequestProperties().containsKey(HEADER_CONTENT_TYPE)) {
            MediaType mediaType = requestBody.contentType();
            if (mediaType != null) {
                System.out.println(mediaType.toString());
                connection.setRequestProperty(HEADER_CONTENT_TYPE, mediaType.toString());
            }

        }
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        requestBody.writeTo(out);
        out.close();
    }

    /**
     * Checks if a response message contains a body.
     *
     * @param requestMethod request method
     * @param responseCode  response status code
     * @return whether the response has a body
     * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.3">RFC 7230 section 3.3</a>
     */
    private static boolean hasResponseBody(String requestMethod, int responseCode) {
        return (!requestMethod.equals("HEAD"))
                && !(HTTP_CONTINUE <= responseCode && responseCode < HttpURLConnection.HTTP_OK)
                && responseCode != HttpURLConnection.HTTP_NO_CONTENT
                && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        return executeRequest(request);
    }

    private Response executeRequest(Request request)
            throws IOException {
        HttpURLConnection connection = openConnection(request);
        Headers headers = request.headers();
        workThread = Thread.currentThread();
        boolean keepConnectionOpen = false;
        try {
            for (int i = 0, size = headers.size(); i < size; i++) {
                connection.addRequestProperty(headers.name(i), headers.value(i));
            }
            setConnectionParametersForRequest(connection, request);
            // Initialize HttpResponse with data from the HttpURLConnection.
            int responseCode = connection.getResponseCode();
            if (responseCode == -1) {
                // -1 is returned by getResponseCode() if the response code could not be retrieved.
                // Signal to the caller that something was wrong with the connection.
                throw new IOException("Could not retrieve response code from HttpUrlConnection.");
            }
            Response response = new Response.Builder()
                    .request(request)
                    .code(responseCode)
                    .message(connection.getResponseMessage())
                    .body(null)
                    .headers(Headers.of(convertHeaders(connection.getHeaderFields())))
                    .build();

            if (!hasResponseBody(request.method(), responseCode)) {
                return response;
            }

            // Need to keep the connection openZipArchive until the stream is consumed by the caller. Wrap the
            // stream such that close() will disconnect the connection.
            keepConnectionOpen = true;
            return response.newBuilder()
                    .body(ResponseBody.create(MediaType.parse(response.header(HEADER_CONTENT_TYPE)), connection.getContentLength(), new UrlConnectionInputStream(connection)))
                    .build();

        } finally {
            if (!keepConnectionOpen) {
                connection.disconnect();
            }
        }
    }

    public synchronized void cancel() {
        cancel = true;
        if (workThread != null) {
            workThread.interrupt();
        }
    }

    public synchronized boolean isCancel() {
        return cancel;
    }

    private HttpURLConnection openConnection(Request request) throws IOException {
        URL url = request.url();
        HttpURLConnection connection = createConnection(url);
        connection.setConnectTimeout(client.connectTimeoutMillis());
        connection.setReadTimeout(client.readTimeoutMillis());
        connection.setUseCaches(false);
        connection.setDoInput(true);
        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol())) {
            HostnameVerifier verifier = request.hostnameVerifier();
            if (verifier == null) {
                verifier = client.hostnameVerifier();
            }

            SSLSocketFactory sslSocketFactory = request.sslSocketFactory();
            if (sslSocketFactory == null) {
                sslSocketFactory = client.sslSocketFactory();
            }
            if (verifier != null) {
                ((HttpsURLConnection) connection).setHostnameVerifier(verifier);
            }
            if (sslSocketFactory != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            }

        }

        return connection;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    protected HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Workaround for the M release HttpURLConnection not observing the
        // HttpURLConnection.setFollowRedirects() property.
        // https://code.google.com/p/android/issues/detail?id=194495
        connection.setInstanceFollowRedirects(HttpURLConnection.getFollowRedirects());
        return connection;
    }

    static class UrlConnectionInputStream extends FilterInputStream {
        private final HttpURLConnection mConnection;

        UrlConnectionInputStream(HttpURLConnection connection) {
            super(inputStreamFromConnection(connection));
            mConnection = connection;
        }

        @Override
        public void close() throws IOException {
            super.close();
            mConnection.disconnect();
        }
    }
}
