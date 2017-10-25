package com.qyh.litemvp.http.interceptor;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Description: 请求头拦截
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 12:21.
 */
public class HeadersInterceptor implements Interceptor {

    private Map<String, String> headers;

    public HeadersInterceptor(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        if (headers != null && headers.size() > 0) {
            Set<String> keys = headers.keySet();
            for (String headerKey : keys) {

                //拦截添加请求头
                builder.addHeader(headerKey, headers.get(headerKey)).build();
            }
        }

        // 返回添加请求头后的response
        return chain.proceed(builder.build());
    }
}
