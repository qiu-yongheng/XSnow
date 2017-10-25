package com.qyh.litemvp.http.core;

import android.content.Context;

import com.qyh.litemvp.common.ViseConfig;
import com.qyh.litemvp.http.mode.ApiHost;
import com.vise.log.ViseLog;
import com.qyh.litemvp.cache.DiskCache;
import com.qyh.litemvp.http.mode.CacheMode;
import com.qyh.litemvp.http.mode.CacheResult;
import com.qyh.litemvp.http.strategy.ICacheStrategy;

import java.io.File;
import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @Description: 针对响应数据进行缓存管理 (磁盘缓存)
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:27.
 */
public class ApiCache {
    private final DiskCache diskCache;
    private String cacheKey;

    /**
     * 自定义的被订阅者
     * @param <T>
     */
    private static abstract class SimpleSubscribe<T> implements ObservableOnSubscribe<T> {
        @Override
        public void subscribe(ObservableEmitter<T> subscriber) throws Exception {
            try {
                // 获取 数据
                T data = execute();
                if (!subscriber.isDisposed() && data != null) {
                    subscriber.onNext(data);
                }
            } catch (Throwable e) {
                ViseLog.e(e);
                Exceptions.throwIfFatal(e);
                if (!subscriber.isDisposed()) {
                    subscriber.onError(e);
                }
                return;
            }
            if (!subscriber.isDisposed()) {
                subscriber.onComplete();
            }
        }

        abstract T execute() throws Throwable;
    }

    private ApiCache(Context context, String cacheKey, long time) {
        this.cacheKey = cacheKey;
        this.diskCache = new DiskCache(context).setCacheTime(time);
    }

    private ApiCache(Context context, File diskDir, long diskMaxSize, String cacheKey, long time) {
        this.cacheKey = cacheKey;
        diskCache = new DiskCache(context, diskDir, diskMaxSize).setCacheTime(time);
    }

    /**
     * 转换成指定缓存策略的class, 调用指定缓存的execute方法将数据转换成CacheResult
     * @param cacheMode 缓存策略枚举
     * @param type
     * @param <T>
     * @return
     */
    public <T> ObservableTransformer<T, CacheResult<T>> transformer(CacheMode cacheMode, final Type type) {
        final ICacheStrategy strategy = loadStrategy(cacheMode);//获取缓存策略
        return new ObservableTransformer<T, CacheResult<T>>() {
            @Override
            public ObservableSource<CacheResult<T>> apply(Observable<T> apiResultObservable) {
                ViseLog.i("cacheKey=" + ApiCache.this.cacheKey);
                return strategy.execute(ApiCache.this, ApiCache.this.cacheKey, apiResultObservable, type);
            }
        };
    }

    /**
     * 获取数据
     * @param key
     * @return
     */
    public Observable<String> get(final String key) {
        return Observable.create(new SimpleSubscribe<String>() {
            @Override
            String execute() {
                return diskCache.get(key);
            }
        });
    }

    /**
     * 保存缓存
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public <T> Observable<Boolean> put(final String key, final T value) {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                diskCache.put(key, value);
                return true;
            }
        });
    }

    /**
     * 是否包含key对应的缓存
     * @param key
     * @return
     */
    public boolean containsKey(final String key) {
        return diskCache.contains(key);
    }

    /**
     * 移除key对应的缓存
     * @param key
     */
    public void remove(final String key) {
        diskCache.remove(key);
    }

    /**
     * 缓存是否关闭
     * @return
     */
    public boolean isClosed() {
        return diskCache.isClosed();
    }

    /**
     * 清空缓存
     * @return
     */
    public Disposable clear() {
        return Observable.create(new SimpleSubscribe<Boolean>() {
            @Override
            Boolean execute() throws Throwable {
                diskCache.clear();
                return true;
            }
        }).subscribeOn(Schedulers.io()).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean status) throws Exception {
                ViseLog.i("clear status => " + status);
            }
        });
    }

    /**
     * 根据类全名创建class
     * @param cacheMode 缓存策略枚举, getClassName()可以获取枚举封装的类名
     * @return
     */
    public ICacheStrategy loadStrategy(CacheMode cacheMode) {
        try {
            String pkName = ICacheStrategy.class.getPackage().getName();
            return (ICacheStrategy) Class.forName(pkName + "." + cacheMode.getClassName()).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("loadStrategy(" + cacheMode + ") err!!" + e.getMessage());
        }
    }

    public static final class Builder {
        private final Context context;
        private File diskDir;
        private long diskMaxSize;
        private long cacheTime = ViseConfig.CACHE_NEVER_EXPIRE;
        private String cacheKey = ApiHost.getHost();

        public Builder(Context context) {
            this.context = context;
        }

        public Builder(Context context, File diskDir, long diskMaxSize) {
            this.context = context;
            this.diskDir = diskDir;
            this.diskMaxSize = diskMaxSize;
        }

        public Builder cacheKey(String cacheKey) {
            this.cacheKey = cacheKey;
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public ApiCache build() {
            if (diskDir == null || diskMaxSize == 0) {
                return new ApiCache(context, cacheKey, cacheTime);
            } else {
                return new ApiCache(context, diskDir, diskMaxSize, cacheKey, cacheTime);
            }
        }

    }
}