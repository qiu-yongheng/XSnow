package com.qyh.snowdemo.activity;

import android.os.Bundle;
import android.widget.Toast;

import com.qyh.litemvp.nucleus.presenter.Factory;
import com.qyh.litemvp.nucleus.presenter.RxPresenter;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiConsumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author 邱永恒
 * @time 2017/10/24  17:16
 * @desc ${TODD}
 */

public class ppp extends RxPresenter<aaa>{

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        restartableLatestCache(1, new Factory<Observable<String>>() {
            @Override
            public Observable<String> create() {
                return Observable.create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(ObservableEmitter<String> e) throws Exception {
                        e.onNext("asdf");
                        e.onComplete();
                    }
                });
            }
        }, new BiConsumer<aaa, String>() {
            @Override
            public void accept(aaa aaa, String s) throws Exception {
                Toast.makeText(aaa, s, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void show (String msg) {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) throws Exception {
                e.onNext("gaga");
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.<String>deliverLatestCache())
                .subscribe(this.split(new BiConsumer<aaa, String>() {
                    @Override
                    public void accept(aaa aaa, String s) throws Exception {
                        Toast.makeText(aaa, s, Toast.LENGTH_LONG).show();
                    }
                }, new BiConsumer<aaa, Throwable>() {
                    @Override
                    public void accept(aaa aaa, Throwable throwable) throws Exception {

                    }
                }));


    }
}
