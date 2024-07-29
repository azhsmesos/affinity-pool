package com.github.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 *
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public interface ListenableFuture<V> extends Future<V> {

    void addListener(Runnable listener, Executor executor);
}
