package com.qyh.litemvp.event.inner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * @Description: 事件处理基类
 * @author: <a href="http://www.xiaoyaoyou1212.com">DAWI</a>
 * @date: 2016-12-29 19:08
 */
public class EventBase {
    /**
     * 粘性事件集合
     */
    protected final static Map<Class<?>, Object> STICKY_EVENT_MAP;
    /**
     * 被订阅者
     */
    protected final static Subject<Object> SUBJECT;

    static {
        // toSerialized(): 可以使线程安全
        SUBJECT = PublishSubject.create().toSerialized();
        STICKY_EVENT_MAP = new HashMap<>();
    }

    protected EventBase() {
    }

    /**
     * ofType: 只发射指定类型的数据
     *
     * @param eventType
     * @param <T>
     * @return
     */
    static <T> Flowable<T> toFlowable(Class<T> eventType) {
        return SUBJECT.ofType(eventType).toFlowable(BackpressureStrategy.BUFFER);
    }

    /**
     * 删除指定粘性事件
     *
     * @param event
     */
    static synchronized void dellSticky(Object event) {
        if (!STICKY_EVENT_MAP.isEmpty()) {
            List<Class> classes = new ArrayList<>();
            for (Map.Entry<Class<?>, Object> objectEntry : STICKY_EVENT_MAP.entrySet()) {
                if (objectEntry.getKey() == event.getClass()) {
                    classes.add(event.getClass());
                }
            }
            stickyEventMapRemove(classes);
        }
    }

    static void stickyEventMapRemove(List<Class> classes) {
        for (Class aClass : classes) {
            STICKY_EVENT_MAP.remove(aClass);
        }
    }
}
