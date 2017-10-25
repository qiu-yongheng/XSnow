package com.qyh.litemvp.cache;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import com.qyh.litemvp.common.ViseConfig;
import com.vise.log.ViseLog;
import com.vise.utils.cipher.MD5;
import com.vise.utils.system.AppUtil;
import com.qyh.litemvp.common.GsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 磁盘缓存，KEY加密存储，可定制缓存时长 (框架使用磁盘缓存作为离线缓存)
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 15:10
 */
public class DiskCache implements ICache {
    private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_DISK_CACHE_SIZE = 20 * 1024 * 1024; // 20MB

    private DiskLruCache cache;
    // 正则表达式
    private Pattern compile;
    // 缓存过期时间
    private long cacheTime = ViseConfig.CACHE_NEVER_EXPIRE;

    /**
     * 构造器
     * 1. 获取缓存地址
     * 2. 获取缓存大小
     * @param context
     */
    public DiskCache(Context context) {
        this(context, getDiskCacheDir(context, ViseConfig.CACHE_DISK_DIR),
                calculateDiskCacheSize(getDiskCacheDir(context, ViseConfig.CACHE_DISK_DIR)));
    }

    /**
     * 构造器
     * @param context 上下文
     * @param diskDir 磁盘缓存路径
     * @param diskMaxSize 磁盘最大缓存
     */
    public DiskCache(Context context, File diskDir, long diskMaxSize) {
        /**
         * (): 标记一个表达式
         * d: 数字
         * +: 匹配前面的子表达式一次或多次 例如，'zo+' 能匹配 "zo" 以及 "zoo"，但不能匹配 "z"
         * ?: 匹配前面的子表达式零次或一次。例如，"do(es)?" 可以匹配 "do" 或 "does" 中的"do"
         */
        final String REGEX = "@createTime\\{(\\d+)\\}expireMills\\{((-)?\\d+)\\}@";
        // 正则表达式
        compile = Pattern.compile(REGEX);
        try {
            /** 创建一个DiskLruCache的实例 */
            cache = DiskLruCache.open(diskDir, AppUtil.getVersionCode(context), 1, diskMaxSize);
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    /**
     * 保存数据到缓存
     * @param key key
     * @param value value
     */
    public void put(String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }
        // 1. 将URL转MD5
        String name = getMd5Key(key);
        try {
            if (!TextUtils.isEmpty(get(name))) {
                // 2. 清除缓存
                cache.remove(name);
            }

            // 3.保存数据到缓存中
            DiskLruCache.Editor editor = cache.edit(name);
            final String TAG_CACHE = "@createTime{createTime_v}expireMills{expireMills_v}@";
            // 给数据添加尾部标识
            String content = value + TAG_CACHE.replace("createTime_v", "" + Calendar.getInstance().getTimeInMillis())
                    .replace("expireMills_v", "" + cacheTime);
            // valueCount = 1, 所以index = 0; 保存数据到缓存
            editor.set(0, content);
            editor.commit();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }

    }

    /**
     * 保存对象到缓存
     * @param key
     * @param value
     */
    @Override
    public void put(String key, Object value) {
        put(key, value != null ? GsonUtil.gson().toJson(value) : null);
    }

    /**
     * 获取缓存数据
     * @param key key
     * @return key对应的value
     */
    public String get(String key) {
        try {
            String md5Key = getMd5Key(key);
            DiskLruCache.Snapshot snapshot = cache.get(md5Key);
            if (snapshot != null) {
                String content = snapshot.getString(0);

                if (!TextUtils.isEmpty(content)) {
                    // 正则
                    Matcher matcher = compile.matcher(content);
                    long createTime = 0;
                    long expireMills = 0;
                    while (matcher.find()) {
                        createTime = Long.parseLong(matcher.group(1));
                        expireMills = Long.parseLong(matcher.group(2));
                    }
                    int index = content.indexOf("@createTime");

                    /**
                     * 缓存超时验证:
                     * 如果创建时间 + 超时时间 > 当前时间, 没有超时, 截取数据
                     * 如果超时, 移除缓存
                     */
                    if ((createTime + expireMills > Calendar.getInstance().getTimeInMillis())
                            || expireMills == ViseConfig.CACHE_NEVER_EXPIRE) {
                        return content.substring(0, index);
                    } else {
                        cache.remove(md5Key);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        return null;
    }

    /**
     * 移除缓存
     * @param key
     */
    public void remove(String key) {
        try {
            cache.remove(getMd5Key(key));
        } catch (Exception e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    /**
     * 判断是否包含key对应的缓存
     * @param key
     * @return
     */
    public boolean contains(String key) {
        try {
            DiskLruCache.Snapshot snapshot = cache.get(getMd5Key(key));
            return snapshot != null;
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
        return false;
    }

    /**
     * 关闭缓存
     * @return
     */
    public boolean isClosed() {
        return cache.isClosed();
    }

    /**
     * 删除所有缓存
     */
    public void clear() {
        try {
            cache.delete();
        } catch (IOException e) {
            e.printStackTrace();
            ViseLog.e(e);
        }
    }

    /**
     * 设置缓存过期时间
     * @param cacheTime
     * @return
     */
    public DiskCache setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
    }

    /**
     * 把key转MD5存储
     *
     * 好处:
     * 1. 如果直接用URL保存, 里面会有特殊符号可能不能命名
     * 2. 如果将URL转MD5保存, 就没有特殊符号了
     * @param key
     * @return
     */
    private String getMd5Key(String key) {
        return MD5.getMessageDigest(key.getBytes());
    }

    /**
     * 获取缓存地址
     * @param context
     * @param dirName
     * @return
     */
    private static File getDiskCacheDir(Context context, String dirName) {
        String cachePath;
        if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable())
                && context.getExternalCacheDir() != null) {
            // 当SD卡存在或者SD卡不可被移除
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            // 没有SD卡
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + dirName);
    }

    /**
     * 获取磁盘缓存大小
     *
     * 剩余空间/50
     * @param dir
     * @return
     */
    private static long calculateDiskCacheSize(File dir) {
        long size = 0;
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 获取缓存文件夹所在空间的剩余空间(手机或SD卡)
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            // 可用block数量 * block大小
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }
        return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
    }

}
