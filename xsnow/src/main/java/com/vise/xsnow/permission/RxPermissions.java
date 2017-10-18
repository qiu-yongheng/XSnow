package com.vise.xsnow.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.FragmentManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.vise.log.ViseLog;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

/**
 * @Description: 权限控制
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2017-04-19 20:14
 */
public class RxPermissions {

    static final String TAG = "RxPermissions";

    private RxPermissionsFragment mRxPermissionsFragment;

    public RxPermissions(@NonNull Activity activity) {
        mRxPermissionsFragment = getRxPermissionsFragment(activity);
    }

    private RxPermissionsFragment getRxPermissionsFragment(Activity activity) {
        RxPermissionsFragment rxPermissionsFragment = findRxPermissionsFragment(activity);
        boolean isNewInstance = rxPermissionsFragment == null;

        // 如果没有创建fragment, 新建
        if (isNewInstance) {
            rxPermissionsFragment = new RxPermissionsFragment();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .add(rxPermissionsFragment, TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return rxPermissionsFragment;
    }

    private RxPermissionsFragment findRxPermissionsFragment(Activity activity) {
        return (RxPermissionsFragment) activity.getFragmentManager().findFragmentByTag(TAG);
    }

    /**
     * Map emitted items from the source observable into {@code true} if permissions in parameters
     * are granted, or {@code false} if not.
     * <p>
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    @SuppressWarnings("WeakerAccess")
    public ObservableTransformer<Object, Boolean> ensure(final String... permissions) {
        return new ObservableTransformer<Object, Boolean>() {
            @Override
            public ObservableSource<Boolean> apply(Observable<Object> o) {
                return request(o, permissions)
                        // Transform Observable<Permission> to Observable<Boolean>
                        .buffer(permissions.length)
                        .flatMap(new Function<List<Permission>, Observable<Boolean>>() {
                            @Override
                            public Observable<Boolean> apply(List<Permission> permissions) throws Exception {
                                if (permissions.isEmpty()) {
                                    // Occurs during orientation change, when the subject receives onComplete.
                                    // In that case we don't want to propagate that empty list to the
                                    // subscriber, only the onComplete.
                                    return Observable.empty();
                                }
                                // Return true if all permissions are granted.
                                for (Permission p : permissions) {
                                    if (!p.granted) {
                                        return Observable.just(false);
                                    }
                                }
                                return Observable.just(true);
                            }
                        });
            }
        };
    }

    /**
     * Map emitted items from the source observable into {@link Permission} objects for each
     * permission in parameters.
     * <p>
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    @SuppressWarnings("WeakerAccess")
    public ObservableTransformer<Object, Permission> ensureEach(final String... permissions) {
        return new ObservableTransformer<Object, Permission>() {
            @Override
            public ObservableSource<Permission> apply(Observable<Object> o) {
                return request(o, permissions);
            }
        };
    }

    /**
     * Request permissions immediately, <b>must be invoked during initialization phase
     * of your application</b>.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public Observable<Boolean> request(final String... permissions) {
        return Observable.just(new Object()).compose(ensure(permissions));
    }

    /**
     * Request permissions immediately, <b>must be invoked during initialization phase
     * of your application</b>.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public Observable<Permission> requestEach(final String... permissions) {
        return Observable.just(new Object()).compose(ensureEach(permissions));
    }

    /**
     *
     * @param trigger
     * @param permissions
     * @return
     */
    private Observable<Permission> request(final Observable<?> trigger, final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request/requestEach requires at least one input permission");
        }
        return oneOf(trigger, pending(permissions))
                .flatMap(new Function<Object, Observable<Permission>>() {
                    @Override
                    public Observable<Permission> apply(Object o) throws Exception {
                        return requestImplementation(permissions);
                    }
                });
    }

    /**
     *
     * @param permissions
     * @return
     */
    private Observable<?> pending(final String... permissions) {
        for (String p : permissions) {
            if (!mRxPermissionsFragment.containsByPermission(p)) {
                return Observable.empty();
            }
        }
        //
        return Observable.just(new Object());
    }

    /**
     *
     * @param trigger
     * @param pending
     * @return
     */
    private Observable<?> oneOf(Observable<?> trigger, Observable<?> pending) {
        if (trigger == null) {
            return Observable.just(new Object());
        }

        // merge: 将多个Observables的输出合并
        return Observable.merge(trigger, pending);
    }

    /**
     * 请求权限的具体实现
     * @param permissions
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> requestImplementation(final String... permissions) {
        // 保存所有请求权限的集合
        List<Observable<Permission>> list = new ArrayList<>(permissions.length);
        // 保存未申请的权限集合
        List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create an Observable for each of them.(当请求多个权限的情况下, 创建一个Observable来一个个请求)
        // At the end, the observables are combined to have a unique response.(最后, 把所有的Observable合并成一个任务)
        for (String permission : permissions) {
            ViseLog.i("Requesting permission " + permission);

            // 判断权限是否已授权
            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                // 权限被允许
                list.add(Observable.just(new Permission(permission, true, false)));
                continue;
            }

            // 判断权限是否被注销
            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                // 权限被取消
                list.add(Observable.just(new Permission(permission, false, false)));
                continue;
            }

            // 获取还未申请的权限
            PublishSubject<Permission> subject = mRxPermissionsFragment.getSubjectByPermission(permission);
            // Create a new subject if not exists
            if (subject == null) {
                unrequestedPermissions.add(permission);
                subject = PublishSubject.create();
                // 创建一个申请权限的任务, 并保存到集合中
                mRxPermissionsFragment.setSubjectForPermission(permission, subject);
            }

            // 添加还未申请权限的任务到集合中
            list.add(subject);
        }

        // 如果未申请权限不为空, 申请权限
        if (!unrequestedPermissions.isEmpty()) {
            String[] unrequestedPermissionsArray = unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]);
            // 申请权限
            requestPermissionsFromFragment(unrequestedPermissionsArray);
        }
        // 不交错的发射两个或多个Observable的发射物
        return Observable.concat(Observable.fromIterable(list));
    }

    /**
     * Invokes Activity.shouldShowRequestPermissionRationale and wraps
     * the returned value in an observable.
     * <p>
     * In case of multiple permissions, only emits true if
     * Activity.shouldShowRequestPermissionRationale returned true for
     * all revoked permissions.
     * <p>
     * You shouldn't call this method if all permissions have been granted.
     * <p>
     * For SDK &lt; 23, the observable will always emit false.
     */
    @SuppressWarnings("WeakerAccess")
    public Observable<Boolean> shouldShowRequestPermissionRationale(final Activity activity, final String... permissions) {
        if (!isMarshmallow()) {
            return Observable.just(false);
        }
        return Observable.just(shouldShowRequestPermissionRationaleImplementation(activity, permissions));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean shouldShowRequestPermissionRationaleImplementation(final Activity activity, final String... permissions) {
        for (String p : permissions) {
            if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 请求权限
     * @param permissions
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissionsFromFragment(String[] permissions) {
        ViseLog.i("requestPermissionsFromFragment " + TextUtils.join(", ", permissions));
        // 让fragment调用系统API请求权限
        mRxPermissionsFragment.requestPermissions(permissions);
    }

    /**
     * Returns true if the permission is already granted.
     * 权限是否已授权
     * <p>
     * Always true if SDK &lt; 23.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isGranted(String permission) {
        return !isMarshmallow() || mRxPermissionsFragment.isGranted(permission);
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     * 权限是否已撤销
     * <p>
     * Always false if SDK &lt; 23.
     */
    @SuppressWarnings("WeakerAccess")
    public boolean isRevoked(String permission) {
        return isMarshmallow() && mRxPermissionsFragment.isRevoked(permission);
    }

    /**
     * 判断是否是安卓5.0以上系统
     * @return
     */
    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     *
     * @param permissions
     * @param grantResults
     */
    void onRequestPermissionsResult(String permissions[], int[] grantResults) {
        mRxPermissionsFragment.onRequestPermissionsResult(permissions, grantResults, new boolean[permissions.length]);
    }
}
