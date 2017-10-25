package com.qyh.litemvp.http.interceptor;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.qyh.litemvp.common.ViseConfig;
import com.vise.log.ViseLog;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * @Description: 在线缓存拦截
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 22:23.
 */
public class OnlineCacheInterceptor implements Interceptor {
    private String cacheControlValue;

    public OnlineCacheInterceptor() {
        this(ViseConfig.MAX_AGE_ONLINE);
    }

    public OnlineCacheInterceptor(int cacheControlValue) {
        this.cacheControlValue = String.format("max-age=%d", cacheControlValue);
    }

    /**
     * no-store: 所有内容都不会被缓存到缓存或 Internet 临时文件中
     * no-cache: 所有内容都不会被缓存
     * must-revalidate:  如果缓存的内容失效，请求必须发送到服务器/代理以进行重新验证
     * max-age: 缓存的内容将在 xxx 秒后失效, 这个选项只在HTTP 1.1可用, 并如果和Last-Modified一起使用时, 优先级较高
     *
     * @param chain
     * @return
     * @throws IOException
     */
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        // 获取响应
        Response originalResponse = chain.proceed(chain.request());

        // 获取响应的缓存策略
        String cacheControl = originalResponse.header("Cache-Control");
        if (TextUtils.isEmpty(cacheControl) || cacheControl.contains("no-store") || cacheControl.contains("no-cache") || cacheControl
                .contains("must-revalidate") || cacheControl.contains("max-age") || cacheControl.contains("max-stale")) {
            ViseLog.d(originalResponse.headers());
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, " + cacheControlValue)
                    .removeHeader("Pragma")
                    .build();

        } else {
            return originalResponse;
        }
    }
}
