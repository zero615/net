package com.zero.support.net.internal;


import com.zero.support.net.Interceptor;
import com.zero.support.net.Response;

import java.io.IOException;

public class CacheInterceptor implements Interceptor {
    @Override
    public com.zero.support.net.Response intercept(Chain chain) throws IOException {
        //todo 实现http 缓存
        return chain.proceed(chain.request());
    }
}
