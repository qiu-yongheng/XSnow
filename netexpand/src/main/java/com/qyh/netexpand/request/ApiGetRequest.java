package com.qyh.netexpand.request;

import com.qyh.netexpand.func.ApiResultFunc;
import com.qyh.litemvp.http.ViseHttp;
import com.qyh.litemvp.http.callback.ACallback;
import com.qyh.litemvp.http.core.ApiManager;
import com.qyh.litemvp.http.mode.CacheResult;
import com.qyh.litemvp.http.subscriber.ApiCallbackSubscriber;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;

/**
 * @Description: 返回APIResult的GET请求类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/5/13 14:31.
 */
public class ApiGetRequest extends ApiBaseRequest {
    public ApiGetRequest(String suffixUrl) {
        super(suffixUrl);
    }

    @Override
    protected <T> Observable<T> execute(Type type) {

        return apiService
                .get(suffixUrl, params)
                // ResponseBody转ApiResult<T>
                .map(new ApiResultFunc<T>(type))
                // ApiResult<T>转T
                .compose(this.<T>apiTransformer());
    }

    @Override
    protected <T> Observable<CacheResult<T>> cacheExecute(Type type) {
        return this.<T>execute(type).compose(ViseHttp.getApiCache().<T>transformer(cacheMode, type));
    }

    @Override
    protected <T> void execute(ACallback<T> callback) {
        DisposableObserver disposableObserver = new ApiCallbackSubscriber(callback);
        if (super.tag != null) {
            ApiManager.get().add(super.tag, disposableObserver);
        }
        if (isLocalCache) {
            this.cacheExecute(getSubType(callback)).subscribe(disposableObserver);
        }
        this.execute(getType(callback)).subscribe(disposableObserver);
    }
}
