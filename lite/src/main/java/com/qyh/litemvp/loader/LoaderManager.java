package com.qyh.litemvp.loader;

/**
 * @Description: 图片加载管理，可定制图片加载框架
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-19 15:16
 */
public class LoaderManager {
    private static ILoader innerLoader;
    private static ILoader externalLoader;

    /**
     * 设置图片框架
     * @param loader
     */
    public static void setLoader(ILoader loader) {
        if (externalLoader == null && loader != null) {
            externalLoader = loader;
        }
    }

    /**
     * 获取当前图片框架
     * 1. 框架默认使用glide进行图片加载
     * 2. 如果要使用其他图片框架, 可以随意切换
     * @return
     */
    public static ILoader getLoader() {
        if (innerLoader == null) {
            synchronized (LoaderManager.class) {
                if (innerLoader == null) {
                    if (externalLoader != null) {
                        innerLoader = externalLoader;
                    } else {
                        innerLoader = new GlideLoader();
                    }
                }
            }
        }
        return innerLoader;
    }
}
