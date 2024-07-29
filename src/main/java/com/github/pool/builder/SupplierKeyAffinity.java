package com.github.pool.builder;

import static com.github.pool.util.MoreFunctions.lazy;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.github.pool.KeyAffinity;
import com.github.pool.util.CloseableSupplier;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class SupplierKeyAffinity<K, V> implements KeyAffinity<K, V> {

    private final CloseableSupplier<KeyAffinityRelation<K, V>> factory;

    public SupplierKeyAffinity(Supplier<KeyAffinityRelation<K, V>> factory) {
        this.factory = lazy(factory, false);
    }

    public V select(K key) {
        return factory.get().select(key);
    }

    public void finishCall(K key) {
        factory.get().finishCall(key);
    }

    @Override
    public boolean init() {
        return false;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Iterator<V> iterator() {
        return null;
    }
}
