package com.vise.xsnow.cache;

/**
 * @Description: 缓存接口
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2016-12-19 15:03
 */
public interface ICache {
    /**
     * 添加缓存
     * @param key
     * @param value
     */
    void put(String key, Object value);

    /**
     * 获取缓存
     * @param key
     * @return
     */
    Object get(String key);

    /**
     * 判断是否包含key对应的缓存
     * @param key
     * @return
     */
    boolean contains(String key);

    /**
     * 移除key对应的缓存
     * @param key
     */
    void remove(String key);

    /**
     * 清除所有缓存
     */
    void clear();
}
