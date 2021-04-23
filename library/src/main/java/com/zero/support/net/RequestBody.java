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

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;


public abstract class RequestBody {
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    /**
     * Returns a new request body that transmits {@code content}. If {@code contentType} is non-null
     * and lacks a charset, this will use UTF-8.
     */
    public static com.zero.support.net.RequestBody create(com.zero.support.net.MediaType contentType, String content) {
        Charset charset = UTF_8;
        if (contentType != null) {
            charset = contentType.charset();
            if (charset == null) {
                charset = UTF_8;
                contentType = com.zero.support.net.MediaType.parse(contentType + "; charset=utf-8");
            }
        }
        byte[] bytes = content.getBytes(charset);
        return create(contentType, bytes);
    }

    /**
     * Returns a new request body that transmits {@code content}.
     */
    public static com.zero.support.net.RequestBody create(final com.zero.support.net.MediaType contentType, final byte[] content) {
        return create(contentType, content, 0, content.length);
    }

    /**
     * Returns a new request body that transmits {@code content}.
     */
    public static com.zero.support.net.RequestBody create(final com.zero.support.net.MediaType contentType, final byte[] content,
                                                                 final int offset, final int byteCount) {
        if (content == null) throw new NullPointerException("content == null");
        Util.checkOffsetAndCount(content.length, offset, byteCount);
        return new com.zero.support.net.RequestBody() {
            @Override
            public com.zero.support.net.MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return byteCount;
            }

            @Override
            public void writeTo(OutputStream stream) throws IOException {
                new DataOutputStream(stream).write(content);
            }


        };
    }

    /**
     * Returns a new request body that transmits the content of {@code file}.
     */
    public static com.zero.support.net.RequestBody create(final com.zero.support.net.MediaType contentType, final File file) {
        if (file == null) throw new NullPointerException("file == null");

        return new com.zero.support.net.RequestBody() {
            @Override
            public com.zero.support.net.MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @Override
            public void writeTo(OutputStream stream) throws IOException {
                //todo 实现文件的上传
                InputStream inputStream = new FileInputStream(file);
                try {
                    IOUtils.copy(inputStream, stream);
                } finally {
                    if (inputStream!=null){
                        try {
                            stream.close();
                        }catch (Throwable e){
                            //ignore
                        }

                    }
                }
            }
        };
    }

    /**
     * Returns the Content-Type header for this body.
     */
    public abstract com.zero.support.net.MediaType contentType();

    public long contentLength() throws IOException {
        return -1;
    }

    public abstract void writeTo(OutputStream stream) throws IOException;
}
