package com.github.pool.util;

import static java.util.Objects.requireNonNull;

/**
 * 链式调用
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
@FunctionalInterface
public interface ThrowableConsumer<T, X extends Throwable> {

    void accept(T t) throws X;

    default ThrowableConsumer<T, X> then(ThrowableConsumer<? super T, X> after) {
        requireNonNull(after);
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}
