package com.zero.support.net.internal;


import com.zero.support.net.Interceptor;
import com.zero.support.net.Request;
import com.zero.support.net.Response;

import java.io.IOException;

public class ConnectInterceptor implements com.zero.support.net.Interceptor {

    @Override
    public com.zero.support.net.Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        return chain.proceed(request);
    }
}
