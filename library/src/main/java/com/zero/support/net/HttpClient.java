/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zero.support.net;


import com.zero.support.net.internal.Util;

import java.net.Proxy;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class HttpClient implements Cloneable, Call.Factory {
    final List<Interceptor> interceptors;
    final List<Interceptor> networkInterceptors;
    final SocketFactory socketFactory;
    final SSLSocketFactory sslSocketFactory;
    final javax.net.ssl.HostnameVerifier hostnameVerifier;


    final boolean followSslRedirects;
    final boolean followRedirects;
    final boolean retryOnConnectionFailure;
    final int callTimeout;
    final int connectTimeout;
    final int readTimeout;
    final int writeTimeout;

    public HttpClient() {
        this(new Builder());
    }

    HttpClient(Builder builder) {

        this.interceptors = Util.immutableList(builder.interceptors);
        this.networkInterceptors = Util.immutableList(builder.networkInterceptors);

        this.socketFactory = builder.socketFactory;
        if (builder.sslSocketFactory != null) {
            this.sslSocketFactory = builder.sslSocketFactory;
        } else {
            this.sslSocketFactory = null;
        }
        this.hostnameVerifier = builder.hostnameVerifier;
        this.followSslRedirects = builder.followSslRedirects;
        this.followRedirects = builder.followRedirects;
        this.retryOnConnectionFailure = builder.retryOnConnectionFailure;
        this.callTimeout = builder.callTimeout;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.writeTimeout = builder.writeTimeout;

        if (interceptors.contains(null)) {
            throw new IllegalStateException("Null interceptor: " + interceptors);
        }
        if (networkInterceptors.contains(null)) {
            throw new IllegalStateException("Null network interceptor: " + networkInterceptors);
        }
    }

    private static SSLSocketFactory newSslSocketFactory(X509TrustManager trustManager) {
        try {
            SSLContext sslContext = SSLContext.getDefault();
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Default call timeout (in milliseconds). By default there is no timeout for complete calls, but
     * there is for the connect, write, and read actions within a call.
     */
    public int callTimeoutMillis() {
        return callTimeout;
    }

    /**
     * Default connect timeout (in milliseconds). The default is 10 seconds.
     */
    public int connectTimeoutMillis() {
        return connectTimeout;
    }

    /**
     * Default read timeout (in milliseconds). The default is 10 seconds.
     */
    public int readTimeoutMillis() {
        return readTimeout;
    }

    /**
     * Default write timeout (in milliseconds). The default is 10 seconds.
     */
    public int writeTimeoutMillis() {
        return writeTimeout;
    }


    public SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory;
    }

    public javax.net.ssl.HostnameVerifier hostnameVerifier() {
        return hostnameVerifier;
    }


    public boolean followSslRedirects() {
        return followSslRedirects;
    }

    public boolean followRedirects() {
        return followRedirects;
    }

    public boolean retryOnConnectionFailure() {
        return retryOnConnectionFailure;
    }

    public List<Interceptor> interceptors() {
        return interceptors;
    }


    public List<Interceptor> networkInterceptors() {
        return networkInterceptors;
    }

    /**
     * Prepares the {@code request} to be executed at some point in the future.
     */
    @Override
    public Call newCall(Request request) {
        return com.zero.support.net.RealCall.newRealCall(this, request);
    }


    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {

        final List<Interceptor> interceptors = new ArrayList<>();
        final List<Interceptor> networkInterceptors = new ArrayList<>();
        Proxy proxy;
        SocketFactory socketFactory;
        SSLSocketFactory sslSocketFactory;

        javax.net.ssl.HostnameVerifier hostnameVerifier;


        boolean followSslRedirects;
        boolean followRedirects;
        boolean retryOnConnectionFailure;
        int callTimeout;
        int connectTimeout;
        int readTimeout;
        int writeTimeout;
        int pingInterval;

        public Builder() {
            socketFactory = SocketFactory.getDefault();
            hostnameVerifier = com.zero.support.net.HostnameVerifier.INSTANCE;
            followSslRedirects = true;
            followRedirects = true;
            retryOnConnectionFailure = true;
            callTimeout = 0;
            connectTimeout = 10_000;
            readTimeout = 10_000;
            writeTimeout = 10_000;
            pingInterval = 0;
        }

        Builder(com.zero.support.net.HttpClient httpClient) {
            this.interceptors.addAll(httpClient.interceptors);
            this.networkInterceptors.addAll(httpClient.networkInterceptors);
            this.socketFactory = httpClient.socketFactory;
            this.sslSocketFactory = httpClient.sslSocketFactory;
            this.hostnameVerifier = httpClient.hostnameVerifier;
            this.followSslRedirects = httpClient.followSslRedirects;
            this.followRedirects = httpClient.followRedirects;
            this.retryOnConnectionFailure = httpClient.retryOnConnectionFailure;
            this.callTimeout = httpClient.callTimeout;
            this.connectTimeout = httpClient.connectTimeout;
            this.readTimeout = httpClient.readTimeout;
            this.writeTimeout = httpClient.writeTimeout;
        }


        public Builder callTimeout(long timeout, TimeUnit unit) {
            callTimeout = Util.checkDuration("timeout", timeout, unit);
            return this;
        }

        public Builder connectTimeout(long timeout, TimeUnit unit) {
            connectTimeout = Util.checkDuration("timeout", timeout, unit);
            return this;
        }


        public Builder readTimeout(long timeout, TimeUnit unit) {
            readTimeout = Util.checkDuration("timeout", timeout, unit);
            return this;
        }

        public Builder writeTimeout(long timeout, TimeUnit unit) {
            writeTimeout = Util.checkDuration("timeout", timeout, unit);
            return this;
        }


        public Builder socketFactory(SocketFactory socketFactory) {
            if (socketFactory == null) throw new NullPointerException("socketFactory == null");
            if (socketFactory instanceof SSLSocketFactory) {
                throw new IllegalArgumentException("socketFactory instanceof SSLSocketFactory");
            }
            this.socketFactory = socketFactory;
            return this;
        }

        public Builder sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            if (sslSocketFactory == null)
                throw new NullPointerException("sslSocketFactory == null");
            this.sslSocketFactory = sslSocketFactory;

            return this;
        }

        public Builder sslSocketFactory(
                SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
            if (sslSocketFactory == null)
                throw new NullPointerException("sslSocketFactory == null");
            if (trustManager == null) throw new NullPointerException("trustManager == null");
            this.sslSocketFactory = sslSocketFactory;

            return this;
        }

        /**
         * Sets the verifier used to confirm that response certificates apply to requested hostnames for
         * HTTPS connections.
         *
         * <p>If unset, a default hostname verifier will be used.
         */
        public Builder hostnameVerifier(javax.net.ssl.HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier == null)
                throw new NullPointerException("hostnameVerifier == null");
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }


        /**
         * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
         *
         * <p>If unset, protocol redirects will be followed. This is different than the built-in {@code
         * HttpURLConnection}'s default.
         */
        public Builder followSslRedirects(boolean followProtocolRedirects) {
            this.followSslRedirects = followProtocolRedirects;
            return this;
        }

        /**
         * Configure this client to follow redirects. If unset, redirects will be followed.
         */
        public Builder followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        /**
         * Returns a modifiable list of interceptors that observe the full span of each call: from
         * before the connection is established (if any) until after the response source is selected
         * (either the origin server, cache, or both).
         */
        public List<Interceptor> interceptors() {
            return interceptors;
        }

        public Builder addInterceptor(Interceptor interceptor) {
            if (interceptor == null) throw new IllegalArgumentException("interceptor == null");
            interceptors.add(interceptor);
            return this;
        }

        public List<Interceptor> networkInterceptors() {
            return networkInterceptors;
        }

        public Builder addNetworkInterceptor(Interceptor interceptor) {
            if (interceptor == null) throw new IllegalArgumentException("interceptor == null");
            networkInterceptors.add(interceptor);
            return this;
        }


        public com.zero.support.net.HttpClient build() {
            return new com.zero.support.net.HttpClient(this);
        }
    }
}
