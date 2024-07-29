package com.github.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public interface MyListeningExecutorService extends ExecutorService {

    <T> ListenableFuture<T> submit(Callable<T> task);

    ListenableFuture<?> submit(Runnable task);
}
