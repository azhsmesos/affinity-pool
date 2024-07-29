package com.github.pool.util;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class MoreFunctions {

    private static final Logger logger = LoggerFactory.getLogger(MoreFunctions.class);

    public static void catchingIfFalse(boolean condition, Runnable runnable) {
        try {
            if (!condition) {
                runnable.run();
            }
        } catch (Exception e) {
            logger.error("[fail safe] ", e);
        }
    }

    public static <T> CloseableSupplier<T> lazy(Supplier<T> delegate, boolean resetAfterCLose) {
        if (delegate instanceof CloseableSupplier) {
            return (CloseableSupplier<T>) delegate;
        }
        requireNonNull(delegate);
        return new CloseableSupplier<>(delegate, resetAfterCLose);
    }

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void sleepUninterruptibly(long sleepFor, TimeUnit unit) {
        boolean interrupted = false;
        try {
            long remainingNanos = unit.toNanos(sleepFor);
            long end = System.nanoTime() + remainingNanos;
            while (true) {
                try {
                    // TimeUnit.sleep() treats negative timeouts just like zero.
                    NANOSECONDS.sleep(remainingNanos);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    remainingNanos = end - System.nanoTime();
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
