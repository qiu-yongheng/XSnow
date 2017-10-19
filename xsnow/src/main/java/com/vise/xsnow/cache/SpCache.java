package com.vise.xsnow.cache;

import android.content.Context;
import android.content.SharedPreferences;

import com.vise.log.ViseLog;
import com.vise.utils.cipher.BASE64;
import com.vise.utils.convert.ByteUtil;
import com.vise.utils.convert.HexUtil;
import com.vise.xsnow.common.ViseConfig;

/**
 * @Description: SharedPreferences存储，支持对象加密存储
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 15:12
 */
public class SpCache implements ICache {
    private SharedPreferences sp;

    public SpCache(Context context) {
        // 创建对象时, 设置缓存地址
        this(context, ViseConfig.CACHE_SP_NAME);
    }

    public SpCache(Context context, String fileName) {
        sp = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public SharedPreferences getSp() {
        return sp;
    }

    /**
     * 保存数据到SP
     *
     * 可以保存任何数据,
     * @param key key
     * @param ser 保存数据
     */
    @Override
    public void put(String key, Object ser) {
        try {
            ViseLog.i(key + " put: " + ser);
            if (ser == null) {
                sp.edit().remove(key).apply();
            } else {
                byte[] bytes = ByteUtil.objectToByte(ser);
                bytes = BASE64.encode(bytes);
                put(key, HexUtil.encodeHexStr(bytes));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取数据
     * @param key
     * @return
     */
    @Override
    public Object get(String key) {
        try {
            String hex = get(key, null);
            if (hex == null) {
                return null;
            }
            byte[] bytes = HexUtil.decodeHex(hex.toCharArray());
            bytes = BASE64.decode(bytes);
            Object obj = ByteUtil.byteToObject(bytes);
            ViseLog.i(key + " get: " + obj);
            return obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断是否包含key对应的数据
     * @param key
     * @return
     */
    @Override
    public boolean contains(String key) {
        return sp.contains(key);
    }

    /**
     * 移除缓存
     * @param key
     */
    @Override
    public void remove(String key) {
        sp.edit().remove(key).apply();
    }

    /**
     * 清空缓存
     */
    @Override
    public void clear() {
        sp.edit().clear().apply();
    }

    /**
     * 保存String
     * @param key
     * @param value
     */
    public void put(String key, String value) {
        if (value == null) {
            sp.edit().remove(key).apply();
        } else {
            sp.edit().putString(key, value).apply();
        }
    }

    /**
     * 保存Boolean
     * @param key
     * @param value
     */
    public void put(String key, boolean value) {
        sp.edit().putBoolean(key, value).apply();
    }

    /**
     * 保存float
     * @param key
     * @param value
     */
    public void put(String key, float value) {
        sp.edit().putFloat(key, value).apply();
    }

    /**
     * 保存long
     * @param key
     * @param value
     */
    public void put(String key, long value) {
        sp.edit().putLong(key, value).apply();
    }

    /**
     * 保存int
     * @param key
     * @param value
     */
    public void putInt(String key, int value) {
        sp.edit().putInt(key, value).apply();
    }

    /**
     * 获取String
     * @param key
     * @param defValue
     * @return
     */
    public String get(String key, String defValue) {
        return sp.getString(key, defValue);
    }

    /**
     * 获取 Boolean
     * @param key
     * @param defValue
     * @return
     */
    public boolean get(String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }

    /**
     * 获取float
     * @param key
     * @param defValue
     * @return
     */
    public float get(String key, float defValue) {
        return sp.getFloat(key, defValue);
    }

    /**
     * 获取int
     * @param key
     * @param defValue
     * @return
     */
    public int getInt(String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    /**
     * 获取long
     * @param key
     * @param defValue
     * @return
     */
    public long get(String key, long defValue) {
        return sp.getLong(key, defValue);
    }
}
