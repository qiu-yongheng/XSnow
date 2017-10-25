package com.qyh.litemvp.http.request;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.qyh.litemvp.http.ViseHttp;
import com.qyh.litemvp.http.callback.ACallback;
import com.qyh.litemvp.common.ViseConfig;
import com.qyh.litemvp.http.core.ApiManager;
import com.qyh.litemvp.http.func.ApiRetryFunc;
import com.qyh.litemvp.http.mode.CacheResult;
import com.qyh.litemvp.http.mode.DownProgress;
import com.qyh.litemvp.http.subscriber.DownCallbackSubscriber;

import org.reactivestreams.Publisher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * @Description: 下载请求
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 17/5/14 21:50.
 */
public class DownloadRequest extends BaseHttpRequest<DownloadRequest> {

    private String dirName = ViseConfig.DEFAULT_DOWNLOAD_DIR;
    private String fileName = ViseConfig.DEFAULT_DOWNLOAD_FILE_NAME;

    /**
     * 设置URL后缀
     * @param suffixUrl
     */
    public DownloadRequest(String suffixUrl) {
        super(suffixUrl);
    }

    /**
     * 设置保存地址
     * @param dirName
     * @return
     */
    public DownloadRequest setDirName(String dirName) {
        if (!TextUtils.isEmpty(dirName)) {
            this.dirName = dirName;
        }
        return this;
    }

    /**
     * 获取保存文件名
     * @param fileName
     * @return
     */
    public DownloadRequest setFileName(String fileName) {
        if (!TextUtils.isEmpty(fileName)) {
            this.fileName = fileName;
        }
        return this;
    }

    @Override
    protected <T> Observable<T> execute(Type type) {
        return (Observable<T>) apiService
                .downFile(suffixUrl, params)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .toFlowable(BackpressureStrategy.LATEST)
                // 将responseBody解析保存的指定文件中
                .flatMap(new Function<ResponseBody, Publisher<?>>() {
                    @Override
                    public Publisher<?> apply(final ResponseBody responseBody) throws Exception {
                        return Flowable.create(new FlowableOnSubscribe<DownProgress>() {
                            @Override
                            public void subscribe(FlowableEmitter<DownProgress> subscriber) throws Exception {
                                // 下载路径
                                File dir = getDiskCacheDir(ViseHttp.getContext(), dirName);
                                if (!dir.exists()) {
                                    dir.mkdir();
                                }

                                // 下载文件
                                File file = new File(dir.getPath() + File.separator + fileName);
                                saveFile(subscriber, file, responseBody);
                            }
                        }, BackpressureStrategy.LATEST);
                    }
                })
                .sample(1, TimeUnit.SECONDS) // 指定的时间间隔定时采样
                .observeOn(AndroidSchedulers.mainThread())
                .toObservable()
                .retryWhen(new ApiRetryFunc(retryCount, retryDelayMillis));
    }

    @Override
    protected <T> Observable<CacheResult<T>> cacheExecute(Type type) {
        return null;
    }

    @Override
    protected <T> void execute(ACallback<T> callback) {
        DisposableObserver disposableObserver = new DownCallbackSubscriber(callback);
        if (super.tag != null) {
            ApiManager.get().add(super.tag, disposableObserver);
        }
        this.execute(getType(callback)).subscribe(disposableObserver);
    }

    /**
     * 下载文件
     * TODO 没有实现断点下载
     * @param sub
     * @param saveFile
     * @param resp
     */
    private void saveFile(FlowableEmitter<? super DownProgress> sub, File saveFile, ResponseBody resp) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                int readLen;
                int downloadSize = 0;
                byte[] buffer = new byte[8192];

                // 记录下载进度
                DownProgress downProgress = new DownProgress();
                inputStream = resp.byteStream();
                outputStream = new FileOutputStream(saveFile);

                // 总长度
                long contentLength = resp.contentLength();
                downProgress.setTotalSize(contentLength);

                while ((readLen = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, readLen);
                    // 记录下载长度
                    downloadSize += readLen;
                    downProgress.setDownloadSize(downloadSize);

                    // 更新进度
                    sub.onNext(downProgress);
                }
                outputStream.flush();
                sub.onComplete();
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
                if (resp != null) {
                    resp.close();
                }
            }
        } catch (IOException e) {
            sub.onError(e);
        }
    }

    /**
     * 获取缓存目录
     * @param context
     * @param dirName
     * @return
     */
    private File getDiskCacheDir(Context context, String dirName) {
        String cachePath;
        if ((Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable())
                && context.getExternalCacheDir() != null) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        // separator : ('/')
        return new File(cachePath + File.separator + dirName);
    }
}
