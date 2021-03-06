package com.qyh.litemvp.http.func;

import com.vise.log.ViseLog;
import com.qyh.litemvp.http.exception.ApiException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * @Description: 重试机制
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-05-04 17:19
 */
public class ApiRetryFunc implements Function<Observable<? extends Throwable>, Observable<?>> {

    private final int maxRetries;
    private final int retryDelayMillis;
    private int retryCount;

    public ApiRetryFunc(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public Observable<?> apply(Observable<? extends Throwable> observable) throws Exception {
        return observable
                .flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) throws Exception {

                        // 如果异常是连接异常, 进行重试
                        if (++retryCount <= maxRetries && (throwable instanceof SocketTimeoutException || throwable instanceof ConnectException)) {
                            ViseLog.d("get response data error, it will try after " + retryDelayMillis
                                    + " millisecond, retry count " + retryCount);

                            // 在指定时间后, 发射 0 , 结合retryWhen操作符, 如果有数据进行发射, 就进行重新订阅
                            return Observable.timer(retryDelayMillis, TimeUnit.MILLISECONDS);
                        }

                        return Observable.error(ApiException.handleException(throwable));
                    }
                });
    }
}
