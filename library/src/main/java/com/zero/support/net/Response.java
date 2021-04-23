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


import java.io.Closeable;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;


/**
 * An HTTP response. Instances of this class are not immutable: the response body is a one-shot
 * value that may be consumed only once and then closed. All other properties are immutable.
 *
 * <p>This class implements {@link Closeable}. Closing it simply closes its response body. See
 * {@link ResponseBody} for an explanation and examples.
 */
public final class Response implements Closeable {
    public static final int HTTP_TEMP_REDIRECT = 307;
    public static final int HTTP_PERM_REDIRECT = 308;
    final Request request;
    final int code;
    final String message;
    final com.zero.support.net.Headers headers;
    final ResponseBody body;

    Response(Builder builder) {
        this.request = builder.request;
        this.code = builder.code;
        this.message = builder.message;
        this.headers = builder.headers.build();
        this.body = builder.body;
    }

    /**
     * The wire-level request that initiated this HTTP response. This is not necessarily the same
     * request issued by the application:
     *
     * <ul>
     * <li>It may be transformed by the HTTP client. For example, the client may copy headers like
     * {@code Content-Length} from the request body.
     * <li>It may be the request generated in response to an HTTP redirect or authentication
     * challenge. In this case the request URL may be different than the initial request URL.
     * </ul>
     */
    public Request request() {
        return request;
    }


    /**
     * Returns the HTTP status code.
     */
    public int code() {
        return code;
    }

    /**
     * Returns true if the code is in [200..300), which means the request was successfully received,
     * understood, and accepted.
     */
    public boolean isSuccessful() {
        return code >= 200 && code < 300;
    }

    /**
     * Returns the HTTP status message.
     */
    public String message() {
        return message;
    }


    public List<String> headers(String name) {
        return headers.values(name);
    }

    public String header(String name) {
        return header(name, null);
    }

    public String header(String name, String defaultValue) {
        String result = headers.get(name);
        return result != null ? result : defaultValue;
    }

    public com.zero.support.net.Headers headers() {
        return headers;
    }

    public ResponseBody body() {
        return body;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    /**
     * Returns true if this response redirects to another resource.
     */
    public boolean isRedirect() {
        switch (code) {
            case HTTP_PERM_REDIRECT:
            case HTTP_TEMP_REDIRECT:
            case HTTP_MULT_CHOICE:
            case HTTP_MOVED_PERM:
            case HTTP_MOVED_TEMP:
            case HTTP_SEE_OTHER:
                return true;
            default:
                return false;
        }
    }


    @Override
    public void close() {
        if (body == null) {
            throw new IllegalStateException("response is not eligible for a body and must not be closed");
        }
        body.close();
    }

    @Override
    public String toString() {
        return "SharedResponse{protocol="

                + ", code="
                + code
                + ", message="
                + message
                + ", url="
                + request.url()
                + '}';
    }

    public static class Builder {
        Request request;

        int code = -1;
        String message;
        com.zero.support.net.Headers.Builder headers;
        ResponseBody body;

        public Builder() {
            headers = new com.zero.support.net.Headers.Builder();
        }

        Builder(com.zero.support.net.Response response) {
            this.request = response.request;
            this.code = response.code;
            this.message = response.message;
            this.headers = response.headers.newBuilder();
            this.body = response.body;

        }

        public Builder request(Request request) {
            this.request = request;
            return this;
        }


        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
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
         * headers like "Set-Cookie".
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

        public Builder body(ResponseBody body) {
            this.body = body;
            return this;
        }

        public com.zero.support.net.Response build() {
            if (request == null) throw new IllegalStateException("request == null");

            if (code < 0) throw new IllegalStateException("code < 0: " + code);
            if (message == null) throw new IllegalStateException("message == null");
            return new com.zero.support.net.Response(this);
        }
    }
}
