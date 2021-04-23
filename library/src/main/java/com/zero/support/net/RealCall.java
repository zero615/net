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


import com.zero.support.net.internal.CacheInterceptor;
import com.zero.support.net.internal.CallServerInterceptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class RealCall implements Call {
    final HttpClient client;
    /**
     * The application's original request unadulterated by redirects or auth headers.
     */
    final Request originalRequest;
    private CallServerInterceptor callServerInterceptor;
    // Guarded by this.
    private boolean executed;

    private RealCall(HttpClient client, Request originalRequest) {
        this.client = client;
        this.originalRequest = originalRequest;
        this.callServerInterceptor = new CallServerInterceptor(client);
    }

    static com.zero.support.net.RealCall newRealCall(HttpClient client, Request originalRequest) {
        return new com.zero.support.net.RealCall(client, originalRequest);
    }

    @Override
    public Request request() {
        return originalRequest;
    }

    @Override
    public Response execute() throws IOException {
        synchronized (this) {
            if (executed) throw new IllegalStateException("Already Executed");
            executed = true;
        }
        return getResponseWithInterceptorChain();
    }

    @Override
    public void cancel() {
        callServerInterceptor.cancel();
    }

    @Override
    public boolean isCanceled() {
        return callServerInterceptor.isCancel();
    }

    @SuppressWarnings("CloneDoesntCallSuperClone")
    // We are a final type & this saves clearing state.
    @Override
    public com.zero.support.net.RealCall clone() {
        return com.zero.support.net.RealCall.newRealCall(client, originalRequest);
    }

    private Response getResponseWithInterceptorChain() throws IOException {
        // Build a full stack of interceptors.
        List<Interceptor> interceptors = new ArrayList<>(client.interceptors());
        interceptors.add(new CacheInterceptor());
        interceptors.addAll(client.networkInterceptors());
        interceptors.add(new CallServerInterceptor(client));
        Interceptor.Chain chain = new RealChain(0, interceptors, originalRequest);
        return chain.proceed(originalRequest);
    }
}
