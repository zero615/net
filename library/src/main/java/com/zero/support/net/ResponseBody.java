/*
 * Copyright (C) 2014 Square, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;


public abstract class ResponseBody implements Closeable {
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Returns a new response body that transmits {@code content}. If {@code contentType} is non-null
     * and lacks a charset, this will use UTF-8.
     */
    public static com.zero.support.net.ResponseBody create(com.zero.support.net.MediaType contentType, String content) {
        Charset charset = UTF_8;
        if (contentType != null) {
            charset = contentType.charset();
            if (charset == null) {
                charset = UTF_8;
                contentType = com.zero.support.net.MediaType.parse(contentType + "; charset=" + charset);
            }
        }

        byte[] bytes = content.getBytes();
        return create(contentType, bytes);
    }

    /**
     * Returns a new response body that transmits {@code content}.
     */
    public static com.zero.support.net.ResponseBody create(final com.zero.support.net.MediaType contentType, byte[] content) {
        return create(contentType, content.length, new ByteArrayInputStream(content));
    }

    /**
     * Returns a new response body that transmits {@code content}.
     */
    public static com.zero.support.net.ResponseBody create(final com.zero.support.net.MediaType contentType,
                                                                  final long contentLength, final InputStream content) {
        if (content == null) throw new NullPointerException("source == null");
        return new com.zero.support.net.ResponseBody() {
            @Override
            public com.zero.support.net.MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return contentLength;
            }

            @Override
            public InputStream source() {
                return content;
            }
        };
    }

    public abstract com.zero.support.net.MediaType contentType();

    /**
     * Returns the number of bytes in that will returned by {@link #bytes}, or {@link #byteStream}, or
     * -1 if unknown.
     */
    public abstract long contentLength();

    public final InputStream byteStream() {
        return source();
    }

    public abstract InputStream source();

    /**
     * Returns the response as a byte array.
     *
     * <p>This method loads entire response body into memory. If the response body is very large this
     * may trigger an {@link OutOfMemoryError}. Prefer to stream the response body if this is a
     * possibility for your response.
     */
    public final byte[] bytes() throws IOException {
        DataInputStream stream = null;
        try {
            stream = new DataInputStream(source());
            long contentLength = contentLength();
            if (contentLength > Integer.MAX_VALUE) {
                throw new IOException("Cannot buffer entire body for content length: " + contentLength);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(contentLength == -1 ? 0 : (int) contentLength);
            byte[] bytes = new byte[4 * 1024];

            int count;
            while ((count = stream.read(bytes)) != -1) {
                output.write(bytes, 0, count);
            }
            if (contentLength != -1 && contentLength != output.size()) {
                throw new IOException("Content-Length ("
                        + contentLength
                        + ") and stream length ("
                        + bytes.length
                        + ") disagree");
            }
            return output.toByteArray();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Returns the response as a string.
     *
     * <p>If the response starts with a <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Byte
     * Order Mark (BOM)</a>, it is consumed and used to determine the charset of the response bytes.
     *
     * <p>Otherwise if the response has a Content-Type header that specifies a charset, that is used
     * to determine the charset of the response bytes.
     *
     * <p>Otherwise the response bytes are decoded as UTF-8.
     *
     * <p>This method loads entire response body into memory. If the response body is very large this
     * may trigger an {@link OutOfMemoryError}. Prefer to stream the response body if this is a
     * possibility for your response.
     */
    public final String string() throws IOException {
        byte[] bytes = bytes();
        return new String(bytes, charset());
    }

    private Charset charset() {
        com.zero.support.net.MediaType contentType = contentType();
        return contentType != null ? contentType.charset(UTF_8) : UTF_8;
    }

    @Override
    public void close() {
        Util.closeQuietly(source());
    }

}
