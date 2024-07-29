package com.github.pool.util;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * 可关闭的supplier
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class CloseableSupplier<T> implements Supplier<T>, Serializable {

    private static final long serialVersionUID = 0L;

    // 原始对象，提供值的首次加载
    private final Supplier<T> delegate;

    // 是否在执行操作时释放缓存
    private final boolean resetAfterClose;

    // 当前是否已经获取过提供器的值
    private transient volatile boolean initialized;

    // 是否关闭
    private transient volatile boolean closing;

    // 当前缓存的提供器的值
    private transient T value;

    public CloseableSupplier(Supplier<T> delegate, boolean resetAfterClose) {
        this.delegate = delegate;
        this.resetAfterClose = resetAfterClose;
    }


    /**
     * 从提供器获取当前值
     */
    @Override
    public T get() {
        if (!this.initialized || closing) {
            synchronized (this) {
                if (!this.initialized) {
                    T value = this.delegate.get();
                    this.value = value;
                    this.initialized = true;
                    return value;
                }
            }
        }
        return this.value;
    }

    public <X extends Throwable> void tryClose(ThrowableConsumer<T, X> close) throws X {
        synchronized (this) {
            if (initialized) {
                if (resetAfterClose) {
                    closing = true;
                }
                try {
                    close.accept(value);
                    if (resetAfterClose) {
                        value = null;
                        initialized = false;
                    }
                } finally {
                    closing = false;
                }
            }
        }
    }
}
