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
 * @Description: Delete请求
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-28 16:06
 */
public class DeleteRequest extends BaseHttpRequest<DeleteRequest> {
    public DeleteRequest(String suffixUrl) {
        super(suffixUrl);
    }

    /**
     * 解析响应数据
     * @param type
     * @param <T>
     * @return
     */
    @Override
    protected <T> Observable<T> execute(Type type) {
        return apiService.delete(suffixUrl, params).compose(this.<T>norTransformer(type));
    }

    /**
     * 把对象转换成CacheResult
     * @param type
     * @param <T>
     * @return
     */
    @Override
    protected <T> Observable<CacheResult<T>> cacheExecute(Type type) {
        return this.<T>execute(type).compose(ViseHttp.getApiCache().<T>transformer(cacheMode, type));
    }

    @Override
    protected <T> void execute(ACallback<T> callback) {
        DisposableObserver disposableObserver = new ApiCallbackSubscriber(callback);
        if (super.tag != null) {
            // 保存请求记录
            ApiManager.get().add(super.tag, disposableObserver);
        }
        if (isLocalCache) {
            this.cacheExecute(getSubType(callback)).subscribe(disposableObserver);
        } else {
            this.execute(getType(callback)).subscribe(disposableObserver);
        }
    }
}
