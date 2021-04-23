/*
 * Copyright (C) 2013 Square, Inc.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;


/**
 * An HTTP request. Instances of this class are immutable if their {@link #body} is null or itself
 * immutable.
 */
public final class Request {
    final URL url;
    final String method;
    final com.zero.support.net.Headers headers;
    final
    com.zero.support.net.RequestBody body;
    final Map<Class<?>, Object> tags;
    final SSLSocketFactory sslSocketFactory;
    final HostnameVerifier hostnameVerifier;

    private volatile com.zero.support.net.CacheControl cacheControl; // Lazily initialized.

    private Request(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers.build();
        this.body = builder.body;
        this.tags = Util.immutableMap(builder.tags);
        this.sslSocketFactory = builder.sslSocketFactory;
        this.hostnameVerifier = builder.hostnameVerifier;
    }

    public SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory;
    }

    public HostnameVerifier hostnameVerifier() {
        return hostnameVerifier;
    }

    public URL url() {
        return url;
    }

    public String method() {
        return method;
    }

    public com.zero.support.net.Headers headers() {
        return headers;
    }

    public String header(String name) {
        return headers.get(name);
    }

    public List<String> headers(String name) {
        return headers.values(name);
    }

    public com.zero.support.net.RequestBody body() {
        return body;
    }

    /**
     * Returns the tag attached with {@code Object.class} as a key, or null if no tag is attached with
     * that key.
     *
     * <p>Prior to OkHttp 3.11, this method never returned null if no tag was attached. Instead it
     * returned either this request, or the request upon which this request was derived with {@link
     * #newBuilder()}.
     */
    public Object tag() {
        return tag(Object.class);
    }

    /**
     * Returns the tag attached with {@code type} as a key, or null if no tag is attached with that
     * key.
     */
    public <T> T tag(Class<? extends T> type) {
        return type.cast(tags.get(type));
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the cache control directives for this response. This is never null, even if this
     * response contains no {@code Cache-Control} header.
     */
    public com.zero.support.net.CacheControl cacheControl() {
        com.zero.support.net.CacheControl result = cacheControl;
        return result != null ? result : (cacheControl = com.zero.support.net.CacheControl.parse(headers));
    }

    public boolean isHttps() {
        return "https".equals(url.getProtocol());
    }

    @Override
    public String toString() {
        return "IntentRequest{method="
                + method
                + ", url="
                + url
                + ", tags="
                + tags
                + '}';
    }

    public static class Builder {

        URL url;
        String method;
        com.zero.support.net.Headers.Builder headers;

        com.zero.support.net.RequestBody body;

        /**
         * A mutable map of tags, or an immutable empty map if we don't have any.
         */
        Map<Class<?>, Object> tags = Collections.emptyMap();
        SSLSocketFactory sslSocketFactory;
        HostnameVerifier hostnameVerifier;

        public Builder() {
            this.method = "GET";
            this.headers = new com.zero.support.net.Headers.Builder();
        }

        Builder(com.zero.support.net.Request request) {
            this.url = request.url;
            this.method = request.method;
            this.body = request.body;
            this.tags = request.tags.isEmpty()
                    ? Collections.<Class<?>, Object>emptyMap()
                    : new LinkedHashMap<>(request.tags);
            this.headers = request.headers.newBuilder();
            this.sslSocketFactory = request.sslSocketFactory;
            this.hostnameVerifier = request.hostnameVerifier;
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

        public Builder hostnameVerifier(HostnameVerifier hostnameVerifier) {
            if (hostnameVerifier == null)
                throw new NullPointerException("hostnameVerifier == null");
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public Builder url(URL url) {
            if (url == null) throw new NullPointerException("url == null");
            this.url = url;
            return this;
        }


        public Builder url(String url) {
            if (url == null) throw new NullPointerException("url == null");

            // Silently replace web socket URLs with HTTP URLs.
            if (url.regionMatches(true, 0, "ws:", 0, 3)) {
                url = "http:" + url.substring(3);
            } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
                url = "https:" + url.substring(4);
            }
            try {
                return url(new URL(url));
            } catch (MalformedURLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }


        /**
         * Sets the header named {@code name} to {@code value}. If this request already has any headers
         * with that name, they are all replaced.
         */
        public Builder header(String name, String value) {
            headers.set(name, value);
            return this;
        }

        /**
         * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
         * headers like "Cookie".
         *
         * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
         * OkHttp may replace {@code value} with a header derived from the request body.
         */
        public Builder addHeader(String name, String value) {
            headers.add(name, value);
            return this;
        }

        /**
         * Removes all headers named {@code name} on this builder.
         */
        public Builder removeHeader(String name) {
            headers.removeAll(name);
            return this;
        }

        /**
         * Removes all headers on this builder and adds {@code headers}.
         */
        public Builder headers(com.zero.support.net.Headers headers) {
            this.headers = headers.newBuilder();
            return this;
        }

        /**
         * todo
         * Sets this request's {@code Cache-Control} header, replacing any cache control headers already
         * present. If {@code cacheControl} doesn't define any directives, this clears this request's
         * cache-control headers.
         */
        public Builder cacheControl(com.zero.support.net.CacheControl cacheControl) {
            String value = cacheControl.toString();
            if (value.isEmpty()) return removeHeader("Cache-Control");
            return header("Cache-Control", value);
        }

        public Builder get() {
            return method("GET", null);
        }

        public Builder head() {
            return method("HEAD", null);
        }

        public Builder post(com.zero.support.net.RequestBody body) {
            return method("POST", body);
        }

        public Builder delete(com.zero.support.net.RequestBody body) {
            return method("DELETE", body);
        }

        public Builder delete() {
            return delete(Util.EMPTY_REQUEST);
        }

        public Builder put(com.zero.support.net.RequestBody body) {
            return method("PUT", body);
        }

        public Builder patch(com.zero.support.net.RequestBody body) {
            return method("PATCH", body);
        }

        public Builder method(String method, com.zero.support.net.RequestBody body) {
            if (method == null) throw new NullPointerException("method == null");
            if (method.length() == 0) throw new IllegalArgumentException("method.length() == 0");
            if (body != null && !com.zero.support.net.HttpMethod.permitsRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must not have a request body.");
            }
            if (body == null && com.zero.support.net.HttpMethod.requiresRequestBody(method)) {
                throw new IllegalArgumentException("method " + method + " must have a request body.");
            }
            this.method = method;
            this.body = body;
            return this;
        }

        /**
         * Attaches {@code tag} to the request using {@code Object.class} as a key.
         */
        public Builder tag(Object tag) {
            return tag(Object.class, tag);
        }

        /**
         * Attaches {@code tag} to the request using {@code type} as a key. Tags can be read from a
         * request using {@link com.zero.support.net.Request#tag}. Use null to remove any existing tag assigned for {@code
         * type}.
         *
         * <p>Use this API to attach timing, debugging, or other application data to a request so that
         * you may read it in interceptors, event listeners, or callbacks.
         */
        public <T> Builder tag(Class<? super T> type, T tag) {
            if (type == null) throw new NullPointerException("type == null");

            if (tag == null) {
                tags.remove(type);
            } else {
                if (tags.isEmpty()) tags = new LinkedHashMap<>();
                tags.put(type, type.cast(tag));
            }

            return this;
        }

        public com.zero.support.net.Request build() {
            if (url == null) throw new IllegalStateException("url == null");
            return new com.zero.support.net.Request(this);
        }
    }
}
