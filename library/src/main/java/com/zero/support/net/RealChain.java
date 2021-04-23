package com.zero.support.net;


import java.io.IOException;
import java.util.List;

public class RealChain implements Interceptor.Chain {
    Request request;
    private List<Interceptor> interceptors;
    private int index;

    public RealChain(int index, List<Interceptor> interceptors, Request request) {
        this.interceptors = interceptors;
        this.index = index;
        this.request = request;
    }

    public Request request() {
        return request;
    }

    public com.zero.support.net.Response proceed(Request request) throws IOException {
        if (index >= interceptors.size()) throw new AssertionError();
        // Call the next interceptor in the chain.
        Interceptor interceptor = interceptors.get(index);
        com.zero.support.net.RealChain next = new com.zero.support.net.RealChain(index + 1, interceptors, request);
        com.zero.support.net.Response response = interceptor.intercept(next);

        // Confirm that the next interceptor made its required call to chain.proceed().
        // Confirm that the intercepted response isn't null.
        if (response == null) {
            throw new NullPointerException("interceptor " + interceptor + " returned null");
        }
        return response;
    }
}
