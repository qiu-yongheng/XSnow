package com.qyh.litemvp.nucleus.presenter.delivery;

import com.qyh.litemvp.nucleus.view.OptionalView;

import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class DeliverLatestCache<View, T> implements ObservableTransformer<T, Delivery<View, T>> {

    private final Observable<OptionalView<View>> view;

    public DeliverLatestCache(Observable<OptionalView<View>> view) {
        this.view = view;
    }

    @Override
    public ObservableSource<Delivery<View, T>> apply(Observable<T> observable) {
        return Observable
                // 当两个Observables中的任何一个发射了数据时，使用一个函数结合每个Observable发射的最近数据项，并且基于这个函数的结果发射数据
                .combineLatest(
                        // 发射view
                        view,
                        // 发射事件
                        observable
                                // RxJava的materialize将来自原始Observable的通知转换为Notification对象，然后它返回的Observable会发射这些数据
                                .materialize()
                                .filter(new Predicate<Notification<T>>() {
                                    @Override
                                    public boolean test(Notification<T> notification) throws Exception {
                                        // 过滤onComplete的通知, 只发射onNext()和onError()
                                        return !notification.isOnComplete();
                                    }
                                }),
                        // 当两个Observables中的任何一个发射了数据时，使用一个函数结合每个Observable发射的最近数据项，并且基于这个函数的结果发射数据
                        new BiFunction<OptionalView<View>, Notification<T>, Object[]>() {
                            @Override
                            public Object[] apply(OptionalView<View> view, Notification<T> notification) throws Exception {
                                return new Object[]{view, notification};
                            }
                        })
                // 顺序变换操作
                .concatMap(new Function<Object[], ObservableSource<Delivery<View, T>>>() {
                    @Override
                    public ObservableSource<Delivery<View, T>> apply(Object[] pack) throws Exception {
                        // 判断是否发射数据 (view != null 且 notification.isOnNext() || notification.isOnError())
                        return Delivery.validObservable((OptionalView<View>) pack[0], (Notification<T>) pack[1]);
                    }
                });
    }
}
