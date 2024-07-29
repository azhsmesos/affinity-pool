package com.github.pool.builder;

import static com.github.pool.util.KeyAffinityExecutorUtils.RANDOM_THRESHOLD;
import static com.github.pool.util.MoreFunctions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.github.pool.util.ThrowableConsumer;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-27
 */
public class KeyAffinityBuilder<V> {

    private Supplier<V> factory;

    private IntSupplier count;

    private ThrowableConsumer<V, Exception> depose;

    private IntPredicate usingRandom;

    private BooleanSupplier counterChecker;

    public <K> SupplierKeyAffinity<K, V> build() {
        ensure();
        return new SupplierKeyAffinity<>(this::buildKeyAffinityRelation);
    }


    public <T extends KeyAffinityBuilder<V>> T depose(ThrowableConsumer<V, Exception> value) {
        requireNonNull(value);
        this.depose = value;
        return (T) this;
    }

    public void ensure() {
        if (count == null || count.getAsInt() <= 0) {
            throw new IllegalArgumentException("no count found.");
        }
        if (counterChecker == null) {
            counterChecker = () -> true;
        }
        if (depose == null) {
            depose = it -> {};
        }
        if (usingRandom == null) {
            usingRandom = it -> it > RANDOM_THRESHOLD;
        }
    }

    public <K> KeyAffinityRelation<K, V> buildKeyAffinityRelation() {
        return new KeyAffinityRelation<>(factory, count, depose, usingRandom, counterChecker);
    }

    public <T extends KeyAffinityBuilder<V>> T count(int value) {
        checkArgument(value > 0);
        this.count = () -> value;
        return (T) this;
    }

    public <T extends KeyAffinityBuilder<V>> T factory(Supplier<V> value) {
        requireNonNull(value);
        this.factory = value;
        return (T) this;
    }
}
