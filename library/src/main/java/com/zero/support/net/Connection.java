package com.zero.support.net;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class Connection {
    public com.zero.support.net.HttpClient client;

    private HttpURLConnection connection;

    public Connection(com.zero.support.net.HttpClient client) {
        this.client = client;
    }

    public HttpURLConnection connect(URL url) throws IOException {
        if (connection == null) {
            connection = openConnection(url);
        }
        return connection;
    }

    private HttpURLConnection openConnection(URL url) throws IOException {
        HttpURLConnection connection = createConnection(url);
        connection.setConnectTimeout(client.connectTimeoutMillis());
        connection.setReadTimeout(client.readTimeoutMillis());
        connection.setUseCaches(false);
        connection.setDoInput(true);
        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol())) {
            SSLSocketFactory sslSocketFactory = client.sslSocketFactory();
            if (sslSocketFactory != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
            }
            HostnameVerifier verifier = client.hostnameVerifier();
            if (verifier != null) {
                ((HttpsURLConnection) connection).setHostnameVerifier(verifier);
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

}
