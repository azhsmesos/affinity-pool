package com.github.pool;

import static com.github.pool.util.ExecutorUtils.executor;

import javax.annotation.Nonnull;

import com.github.pool.builder.KeyAffinityExecutorBuilder;
import com.github.pool.util.ThrowableRunnable;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public interface KeyAffinityExecutor<K> extends KeyAffinity<K, ListeningExecutorService> {

     int DEFAULT_QUEUE_SIZE = 100;



     static <K> KeyAffinityExecutor<K> newSerializerExecutor(int parallelism, String threadName) {
         return newSerializerExecutor(parallelism, DEFAULT_QUEUE_SIZE, threadName);
    }

    /**\
     * 创建亲缘性线程池
     * @param parallelism 并发度
     * @param queueSize 队列长度
     * @param threadName 线程名
     * @return KeyAffinityExecutor
     * @param <K> 定义亲缘key
     */
    static <K> KeyAffinityExecutor<K> newSerializerExecutor(int parallelism, int queueSize, String threadName) {
        return newKeyAffinityExecutor()
                .parallelism(parallelism)
                .executor(executor(threadName, queueSize))
                .build();
    }

    @Nonnull
    static <K> KeyAffinityExecutor<K> newSerializingExecutor(int parallelism, int queueBufferSize, boolean skipDuplicate,
            String threadName) {
        return newKeyAffinityExecutor()
                .parallelism(parallelism)
                .skipDuplicate(skipDuplicate)
                .executor(executor(threadName, queueBufferSize))
                .build();
    }

    static KeyAffinityExecutorBuilder newKeyAffinityExecutor() {
        return new KeyAffinityExecutorBuilder();
    }

    void executeAffinity(K key, ThrowableRunnable<Exception> task);
}
