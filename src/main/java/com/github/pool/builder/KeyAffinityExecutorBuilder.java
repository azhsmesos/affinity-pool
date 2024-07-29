package com.github.pool.builder;

import static com.github.pool.builder.KeyAffinityExecutorStats.wrapStats;
import static com.github.pool.util.ExecutorUtils.shutdownAndAwaitTermination;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.pool.KeyAffinityExecutor;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * 构造器模式
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class KeyAffinityExecutorBuilder {

    private final Map<KeyAffinityExecutor<?>, KeyAffinityExecutor<?>> allExecutors = new ConcurrentHashMap<>();

    private final KeyAffinityBuilder<ListeningExecutorService> builder = new KeyAffinityBuilder<>();

    private boolean usingDynamic = false;

    private boolean shutdownAfterClose = true;

    private boolean skipDuplicate = false;

    /**
     * 创建 {@link KeyAffinityExecutor} 对象
     */
    public <K> KeyAffinityExecutor<K> build() {
        if (usingDynamic && !shutdownAfterClose) {
            throw new IllegalStateException("cannot exec shutdownAfterClose when enable dynamic count.");
        }
        if (shutdownAfterClose) {
            builder.depose(it -> shutdownAndAwaitTermination(it, 1, TimeUnit.DAYS));
        }
        builder.ensure();
        KeyAffinityExecutorRelation<K> relation = new KeyAffinityExecutorRelation<>(builder::buildKeyAffinityRelation);
        relation.setSkipDuplicate(skipDuplicate);
        allExecutors.put(relation, wrapStats(relation));
        return relation;
    }

    public KeyAffinityExecutorBuilder parallelism(int value) {
        builder.count(value);
        return this;
    }

    public KeyAffinityExecutorBuilder skipDuplicate(boolean value) {
        this.skipDuplicate = value;
        return this;
    }

    public KeyAffinityExecutorBuilder executor(Supplier<ExecutorService> factory) {
        requireNonNull(factory);
        builder.factory(() -> {
            ExecutorService executor = factory.get();
            if (executor instanceof ListeningExecutorService) {
                return (ListeningExecutorService) executor;
            } else if (executor instanceof ThreadPoolExecutor) {
                return new ThreadListeningExecutorService((ThreadPoolExecutor) executor);
            } else {
                return listeningDecorator(executor);
            }

        });
        return this;
    }
}
