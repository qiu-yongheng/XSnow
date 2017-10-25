package com.qyh.litemvp.event.inner;

import com.qyh.litemvp.event.Subscribe;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.disposables.CompositeDisposable;

/**
 * @Description: 根据注解查找事件接收方法
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-29 19:26
 */
public class EventFind {
    /**
     * 找到标有注解( @Subscribe() )的订阅者方法
     *
     * @param listenerClass       要寻找的class
     * @param compositeDisposable
     * @return
     */
    public static EventComposite findAnnotatedSubscriberMethods(Object listenerClass, CompositeDisposable compositeDisposable) {
        Set<EventSubscriber> producerMethods = new HashSet<>();
        return findAnnotatedMethods(listenerClass, producerMethods, compositeDisposable);
    }

    /**
     * 找到注解方法
     * <p>
     * 订阅事件
     * <p>
     * 添加订阅事件到compositeDisposable, 方便管理
     *
     * @param listenerClass
     * @param subscriberMethods
     * @param compositeDisposable
     * @return
     */
    private static EventComposite findAnnotatedMethods(Object listenerClass, Set<EventSubscriber> subscriberMethods, CompositeDisposable compositeDisposable) {
        // 找到类中公共的方法
        for (Method method : listenerClass.getClass().getDeclaredMethods()) {
            // 判断方法是否是桥接方法(泛型方法, 在编译时, 会生成一个泛型方法进行强转)
            if (method.isBridge()) {
                continue;
            }

            // 判断方法是否有 Subscribe 注解
            if (method.isAnnotationPresent(Subscribe.class)) {
                // 获取方法的参数类型
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length != 1) {
                    throw new IllegalArgumentException("Method " + method + " has @Subscribe annotation but requires " + parameterTypes
                            .length + " arguments.  Methods must require a single argument.");
                }

                Class<?> parameterClazz = parameterTypes[0];
                // & 判断方法权限是否是 public 0不是, 1是
                if ((method.getModifiers() & Modifier.PUBLIC) == 0) {
                    throw new IllegalArgumentException("Method " + method + " has @EventSubscribe annotation on " + parameterClazz + " " +
                            "but is not 'public'.");
                }

                Subscribe annotation = method.getAnnotation(Subscribe.class);
                ThreadMode thread = annotation.threadMode();

                // 订阅事件
                EventSubscriber subscriberEvent = new EventSubscriber(listenerClass, method, thread);
                if (!subscriberMethods.contains(subscriberEvent)) {
                    subscriberMethods.add(subscriberEvent);//添加事件订阅者
                    compositeDisposable.add(subscriberEvent.getDisposable());//管理订阅，方便取消订阅
                }
            }
        }

        return new EventComposite(compositeDisposable, listenerClass, subscriberMethods);
    }
}
