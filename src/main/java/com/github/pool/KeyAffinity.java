package com.github.pool;

/**
 * 亲缘key接口类
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public interface KeyAffinity<K, V> extends AutoCloseable, Iterable<V> {

    boolean init();
}
