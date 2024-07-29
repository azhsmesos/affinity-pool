package com.github.pool.builder;

import java.util.Iterator;

import com.github.pool.KeyAffinityExecutor;
import com.github.pool.util.ThrowableRunnable;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-28
 */
public class KeyAffinityExecutorStats<K> implements KeyAffinityExecutor<K> {

    private final KeyAffinityExecutor<K> delegate;

    private KeyAffinityExecutorStats(KeyAffinityExecutor<K> delegate) {
        this.delegate = delegate;
    }

    public static <K> KeyAffinityExecutor<K> wrapStats(KeyAffinityExecutor<K> executor) {
        if (executor instanceof KeyAffinityExecutorStats) {
            return executor;
        }
        return new KeyAffinityExecutorStats<>(executor);
    }

    @Override
    public boolean init() {
        return false;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Iterator<ListeningExecutorService> iterator() {
        return null;
    }

    @Override
    public void executeAffinity(K key, ThrowableRunnable<Exception> task) {
        delegate.executeAffinity(key, task);
    }
}
