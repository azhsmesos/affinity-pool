package com.github.pool.builder;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.pool.KeyAffinityExecutor;
import com.github.pool.util.ThrowableRunnable;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.UncheckedExecutionException;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-28
 */
public class KeyAffinityExecutorRelation<K> extends SupplierKeyAffinity<K, ListeningExecutorService> implements
        KeyAffinityExecutor<K> {

    private ConcurrentHashMap<K, SubstituentCallable<?>> substituentTaskMap;

    private boolean skipDuplicate = false;

    public KeyAffinityExecutorRelation(Supplier<KeyAffinityRelation<K, ListeningExecutorService>> factory) {
        super(factory);
    }

    public void setSkipDuplicate(boolean skipDuplicate) {
        this.skipDuplicate = skipDuplicate;
        if (skipDuplicate && substituentTaskMap == null) {
            substituentTaskMap = new ConcurrentHashMap<>();
        }
    }

    @Override
    public void executeAffinity(K key, ThrowableRunnable<Exception> task) {
        requireNonNull(task);
        ThrowableRunnable<Exception> finalTask;
        if (skipDuplicate) {
            Callable<Void> wrapCallable = wrapSkipTask(key, () -> {
                task.run();
                return null;
            });
            if (wrapCallable == null) {
                return;
            }
            finalTask = wrapCallable::call;
        } else {
            finalTask = task;
        }
        // 获取具体ExecutorService
        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        try {
            service.execute(() -> {
                try {
                    finalTask.run();
                } catch (Throwable throwable) {
                    throwIfUnchecked(throwable);
                    throw new UncheckedExecutionException(throwable);
                }
            });
            addCallback = true;
        } finally {
            if (!addCallback) {
                finishCall(key);
            }
        }
    }

    private <T> Callable<T> wrapSkipTask(K key, Callable<T> task) {
        boolean[] firstAdd = {false};
        SubstituentCallable<?> result = substituentTaskMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new SubstituentCallable<>(key, task);
                firstAdd[0] = true;
            } else {
                v.callable = (Callable) task;
            }
            return v;
        });
        if (firstAdd[0]) {
            return (Callable<T>) result;
        }
        return null;
    }

    private class SubstituentCallable<T> implements Callable<T> {

        private final K key;

        private Callable<T> callable;

        private SubstituentCallable(K key, Callable<T> callable) {
            this.key = key;
            this.callable = callable;
        }

        @Override
        public T call() throws Exception {
            substituentTaskMap.remove(key);
            return callable.call();
        }
    }
}
