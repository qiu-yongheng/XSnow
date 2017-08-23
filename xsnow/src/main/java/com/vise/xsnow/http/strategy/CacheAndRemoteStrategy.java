package com.vise.xsnow.http.strategy;

import com.vise.xsnow.http.core.ApiCache;
import com.vise.xsnow.http.mode.CacheResult;

import java.lang.reflect.Type;

import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

/**
 * @Description: 缓存策略--缓存和网络
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 16/12/31 14:33.
 */
public class CacheAndRemoteStrategy<T> extends CacheStrategy<T> {
    @Override
    public <T> Observable<CacheResult<T>> execute(ApiCache apiCache, String cacheKey, Observable<T> source, final Type type) {
        Observable<CacheResult<T>> cache = loadCache(apiCache, cacheKey, type);
        final Observable<CacheResult<T>> remote = loadRemote(apiCache, cacheKey, source);

        // concat: Concat操作符连接多个Observable的输出，就好像它们是一个Observable，第一个Observable发射的所有数据在第二个Observable发射的任何数据前面
        return Observable.concat(cache, remote).filter(new Predicate<CacheResult<T>>() {
            @Override
            public boolean test(CacheResult<T> tCacheResult) throws Exception {
                // 过滤掉没有缓存的数据
                return tCacheResult != null && tCacheResult.getCacheData() != null;
            }
        });
    }
}
