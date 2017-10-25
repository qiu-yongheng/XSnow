package com.qyh.litemvp.http.request;

import com.qyh.litemvp.http.ViseHttp;
import com.qyh.litemvp.http.callback.ACallback;
import com.qyh.litemvp.http.subscriber.ApiCallbackSubscriber;
import com.qyh.litemvp.http.core.ApiManager;
import com.qyh.litemvp.http.mode.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;

/**
 * @Description: Get请求
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-28 16:05
 */
public class GetRequest extends BaseHttpRequest<GetRequest> {
    public GetRequest(String suffixUrl) {
        super(suffixUrl);
    }

    /**
     * 调用: Observable<ResponseBody> get(@Url String url, @QueryMap Map<String, String> maps);
     * 如果没有设置params, 则无参get请求
     *
     * @param type
     * @param <T>
     * @return
     */
    @Override
    protected <T> Observable<T> execute(Type type) {
        // norTransformer: 解析响应数据ResponseBody -> 指定数据类型type
        return apiService.get(suffixUrl, params).compose(this.<T>norTransformer(type));
    }

    /**
     * 请求并缓存数据到本地
     *
     * @param type
     * @param <T>
     * @return
     */
    @Override
    protected <T> Observable<CacheResult<T>> cacheExecute(Type type) {
        // ViseHttp.getApiCache() 获取磁盘缓存对象
        // transformer: 将请求到的数据缓存到本地, 并返回
        return this.<T>execute(type).compose(ViseHttp.getApiCache().<T>transformer(cacheMode, type));
    }

    /**
     * 执行请求
     *
     * @param callback
     * @param <T>
     */
    @Override
    protected <T> void execute(ACallback<T> callback) {
        // 自定义订阅者
        DisposableObserver disposableObserver = new ApiCallbackSubscriber(callback);
        if (super.tag != null) {
            //  添加请求任务到集合, 统一管理
            ApiManager.get().add(super.tag, disposableObserver);
        }
        //判断是否缓存到本地
        if (isLocalCache) {
            this.cacheExecute(getSubType(callback)).subscribe(disposableObserver);
        } else {
            this.execute(getType(callback)).subscribe(disposableObserver);
        }
    }
}
