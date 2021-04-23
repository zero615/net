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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


@SuppressWarnings("all")
public final class FormBody extends RequestBody {
    public static final com.zero.support.net.MediaType CONTENT_TYPE = com.zero.support.net.MediaType.get("application/x-www-form-urlencoded");
    static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
    static final OutputStream EMPTY_OUTPUT_STREAM = new OutputStream() {
        @Override
        public void write(int b) throws IOException {

        }
    };
    private final List<String> encodedNames;
    private final List<String> encodedValues;

    FormBody(List<String> encodedNames, List<String> encodedValues) {
        this.encodedNames = Util.immutableList(encodedNames);
        this.encodedValues = Util.immutableList(encodedValues);
    }

    /**
     * The number of key-value pairs in this form-encoded body.
     */
    public int size() {
        return encodedNames.size();
    }

    public String encodedName(int index) {
        return encodedNames.get(index);
    }

    public String encodedValue(int index) {
        return encodedValues.get(index);
    }


    @Override
    public com.zero.support.net.MediaType contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public long contentLength() throws IOException {
        return writeOrCountBytes(null);
    }


    @Override
    public void writeTo(OutputStream stream) throws IOException {
        writeOrCountBytes(stream);
    }

    private long writeOrCountBytes(OutputStream outputStream) throws IOException {
        DataOutputStream output;
        if (outputStream == null) {
            output = new DataOutputStream(EMPTY_OUTPUT_STREAM);
        } else {
            output = new DataOutputStream(outputStream);
        }
        for (int i = 0, size = encodedNames.size(); i < size; i++) {
            if (i > 0) output.writeByte('&');
            output.writeBytes(encodedNames.get(i));
            output.writeByte('=');
            output.writeBytes(encodedValues.get(i));
        }
        return output.size();
    }

    /**
     * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
     */
    private byte[] encodeParameters(Map<String, String> params, String paramsEncoding) {
        StringBuilder encodedParams = new StringBuilder();
        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "IntentRequest#getParams() or IntentRequest#getPostParams() returned a map "
                                            + "containing a null key or value: (%s, %s). All keys "
                                            + "and values must be non-null.",
                                    entry.getKey(), entry.getValue()));
                }
                encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
                encodedParams.append('=');
                encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
                encodedParams.append('&');
            }
            return encodedParams.toString().getBytes(paramsEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
        }
    }

    public static final class Builder {
        private final List<String> names = new ArrayList<>();
        private final List<String> values = new ArrayList<>();
        private final
        Charset charset;

        public Builder() {
            this(null);
        }

        public Builder(Charset charset) {
            this.charset = charset == null ? Charset.defaultCharset() : charset;
        }

        public Builder add(Map<String, String> param) {
            Set<String> strings = param.keySet();
            for (String key :
                    strings) {
                add(key, param.get(key));
            }
            return this;
        }

        public Builder add(String name, String value) {
            if (name == null) throw new NullPointerException("name == null");
            if (value == null) throw new NullPointerException("value == null");

            try {
                names.add(URLEncoder.encode(name, charset.name()));
                values.add(URLEncoder.encode(value, charset.name()));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

            return this;
        }

        public Builder addEncode(String name, String value) {
            names.add(name);
            values.add(value);
            return this;
        }

        public com.zero.support.net.FormBody build() {
            return new com.zero.support.net.FormBody(names, values);
        }
    }
}
