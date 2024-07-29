package com.github.pool.util;

import static com.github.pool.util.MoreFunctions.catchingIfFalse;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class ExecutorUtils {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtils.class);

    public static boolean shutdownAndAwaitTermination(ExecutorService service, long timeout, TimeUnit unit) {
        long halfTimeout = unit.toNanos(timeout) / 2;
        service.shutdown();
        try {
            if (!service.awaitTermination(halfTimeout, TimeUnit.NANOSECONDS)) {
                service.shutdownNow();
                catchingIfFalse(service.awaitTermination(halfTimeout, TimeUnit.NANOSECONDS),
                        () -> logger.error("Oops. awaitTermination error"));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            service.shutdown();
        }
        return service.isTerminated();
    }

    public static Supplier<ExecutorService> executor(String threadName, int queueBufferSize) {
        return new Supplier<>() {
            private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(threadName)
                    .build();

            @Override
            public ExecutorService get() {
                LinkedBlockingQueue<Runnable> queue;
                if (queueBufferSize > 0) {
                    queue = new LinkedBlockingQueue<>(queueBufferSize) {
                        @Override
                        public boolean offer(Runnable runnable) {
                            try {
                                put(runnable);
                                return true;
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return false;
                        }
                    };
                } else {
                    queue = new LinkedBlockingQueue<>();
                }
                return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, queue, threadFactory);
            }
        };
    }
}
