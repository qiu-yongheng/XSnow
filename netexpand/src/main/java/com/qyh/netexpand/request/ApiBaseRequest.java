package com.qyh.netexpand.request;

import com.qyh.netexpand.func.ApiDataFunc;
import com.qyh.netexpand.mode.ApiResult;
import com.qyh.litemvp.http.func.ApiRetryFunc;
import com.qyh.litemvp.http.request.BaseHttpRequest;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * @Description: 返回APIResult的基础请求类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/5/28 15:46.
 */
public abstract class ApiBaseRequest extends BaseHttpRequest<ApiBaseRequest> {
    public ApiBaseRequest(String suffixUrl) {
        super(suffixUrl);
    }

    /**
     * 服务器返回数据:
     * 1. code
     * 2. message
     * 3. data
     *
     * 如果只关心data, 不关心状态码, 可以转换后返回
     * @param <T>
     * @return
     */
    protected <T> ObservableTransformer<ApiResult<T>, T> apiTransformer() {
        return new ObservableTransformer<ApiResult<T>, T>() {
            @Override
            public ObservableSource<T> apply(Observable<ApiResult<T>> apiResultObservable) {
                return apiResultObservable
                        .subscribeOn(Schedulers.io())
                        .unsubscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        // ApiResult<T>转T
                        .map(new ApiDataFunc<T>())
                        .retryWhen(new ApiRetryFunc(retryCount, retryDelayMillis));
            }
        };
    }
}
