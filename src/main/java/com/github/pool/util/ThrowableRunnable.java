package com.github.pool.util;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-28
 */
@FunctionalInterface
public interface ThrowableRunnable<X extends Throwable> {

    void run() throws X;
}
