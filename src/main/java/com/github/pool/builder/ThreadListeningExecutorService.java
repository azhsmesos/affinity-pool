package com.github.pool.builder;

import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-28
 */
public class ThreadListeningExecutorService extends ForwardingListeningExecutorService {

    private final ThreadPoolExecutor threadPoolExecutor;

    private final ListeningExecutorService wrapped;

    public ThreadListeningExecutorService(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
        this.wrapped = listeningDecorator(threadPoolExecutor);
    }

    @Override
    protected ListeningExecutorService delegate() {
        return wrapped;
    }

    public int getActiveCount() {
        return threadPoolExecutor.getActiveCount();
    }

    public int getMaximumPoolSize() {
        return threadPoolExecutor.getMaximumPoolSize();
    }

    public int getQueueSize() {
        return threadPoolExecutor.getQueue().size();
    }

    public int getQueueRemainingCapacity() {
        return threadPoolExecutor.getQueue().remainingCapacity();
    }
}
