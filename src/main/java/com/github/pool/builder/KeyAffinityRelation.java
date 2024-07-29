package com.github.pool.builder;

import static com.github.pool.util.MoreFunctions.sleepUninterruptibly;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.pool.KeyAffinity;
import com.github.pool.util.ThrowableConsumer;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class KeyAffinityRelation<K, V> implements KeyAffinity<K, V> {

    private static final Logger logger = LoggerFactory.getLogger(KeyAffinityRelation.class);

    private static long sleepBeforeClose = SECONDS.toMillis(5);

    private final IntSupplier count;

    private final List<ValueRef> all;

    private final ThrowableConsumer<V, Exception> deposeFunc;

    private final Map<K, KeyRef> mapping = new ConcurrentHashMap<>();

    private final IntPredicate usingRandom;

    private final BooleanSupplier counterChecker;

    private final Supplier<V> supplier;


    public KeyAffinityRelation(Supplier<V> supplier, IntSupplier count, ThrowableConsumer<V, Exception> deposeFunc,
            IntPredicate usingRandom, BooleanSupplier counterChecker) {
        this.count = count;
        this.usingRandom = usingRandom;
        this.counterChecker = counterChecker;
        this.supplier = supplier;
        this.all = IntStream.range(0, count.getAsInt())
                .mapToObj(it -> supplier.get())
                .map(ValueRef::new)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
        requireNonNull(deposeFunc);
        this.deposeFunc = deposeFunc;
    }

    public void finishCall(K key) {
        mapping.computeIfPresent(key, (k, v) -> {
            if (v.decrConcurrency()) {
                return null;
            }
            return v;
        });
    }

    public V select(K key) {
        int thisCount = count.getAsInt();
        tryCheckValueRefCount(thisCount);
        KeyRef ref = mapping.compute(key, (k, v) -> {
            if (v == null) {
                if (usingRandom.test(thisCount)) {
                    do {
                        try {
                            v = new KeyRef(all.get(ThreadLocalRandom.current().nextInt(all.size())));
                        } catch (IndexOutOfBoundsException e) {
                            // ignore
                        }
                    } while (v == null);
                } else {
                    v = all.stream()
                            .min(Comparator.comparing(ValueRef::concurrency))
                            .map(KeyRef::new)
                            .orElseThrow(IllegalStateException::new);
                }
            }
            v.incrConcurrency();
            return v;
        });
        return ref.ref();
    }

    private void tryCheckValueRefCount(int thisCount) {
        if (!counterChecker.getAsBoolean()) {
            return;
        }
        int toAdd = thisCount - all.size();
        if (toAdd == 0) {
            return;
        }
        synchronized (this) {
            toAdd = thisCount - all.size();
            if (toAdd > 0) {
                all.addAll(IntStream.range(0, toAdd)
                        .mapToObj(it -> supplier.get())
                        .map(ValueRef::new)
                        .toList());
            } else if (toAdd < 0) {
                List<ValueRef> toRemove = new ArrayList<>();
                for (int i = 0; i < -toAdd; i++) {
                    if (all.size() > 0) {
                        ValueRef remove = all.remove(all.size() - 1);
                        toRemove.add(remove);
                    }
                }
                new Thread(() -> {
                    if (sleepBeforeClose > 0) {
                        sleepUninterruptibly(sleepBeforeClose, MILLISECONDS);
                    }
                    for (ValueRef remove : toRemove) {
                        waitAndClose(remove);
                    }
                }, "key-affinity-removal: " + toRemove.size()).start();
            }
        }
    }

    private void waitAndClose(ValueRef remove) {
        while (remove.concurrency() > 0) {
            synchronized (all) {
                try {
                    all.wait(SECONDS.toMillis(1));
                } catch (InterruptedException e) {

                }
            }
        }
        try {
            deposeFunc.accept((V) remove.getObject());
        } catch (Exception e) {
            logger.error("Oops. deposeFunc. ", e);
        }
    }


    @Override
    public boolean init() {
        return true;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Iterator<V> iterator() {
        return null;
    }

    private class KeyRef {
        private final ValueRef valueRef;

        private final AtomicInteger concurrency = new AtomicInteger();

        public KeyRef(ValueRef valueRef) {
            this.valueRef = valueRef;
        }

        public void incrConcurrency() {
            concurrency.incrementAndGet();
            valueRef.incrementAndGet();
        }

        public V ref() {
            return valueRef.getObject();
        }

        public boolean decrConcurrency() {
            int r = concurrency.decrementAndGet();
            int refConcurrency = valueRef.concurrency.decrementAndGet();
            if (refConcurrency <= 0) {
                synchronized (all) {
                    all.notifyAll();
                }
            }
            return r <= 0;
        }
    }

    private class ValueRef {
        private final V object;

        private final AtomicInteger concurrency = new AtomicInteger();

        public ValueRef(V object) {
            this.object = object;
        }

        public int concurrency() {
            return concurrency.get();
        }

        public V getObject() {
            return object;
        }

        public void incrementAndGet() {
            this.concurrency.incrementAndGet();
        }
    }
}
