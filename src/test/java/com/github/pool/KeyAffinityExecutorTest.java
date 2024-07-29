package com.github.pool;


import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;

import org.junit.Test;

/**
 * @author zhaozhenhang <zhaozhenhang@kuaishou.com>
 * Created on 2024-07-28
 */
public class KeyAffinityExecutorTest {



    @Test
    public void testNewSerializerExecutor() {
//        testAffinityExecutor();

        testNormalExecutor();
    }

    private void testAffinityExecutor() {
        List<Person> personList = new ArrayList<>();
        personList.add(new Person(1, "a1"));
        personList.add(new Person(2, "b1"));
        personList.add(new Person(1, "a2"));
        personList.add(new Person(1, "a3"));
        personList.add(new Person(1, "a4"));
        personList.add(new Person(1, "a5"));
        personList.add(new Person(1, "a6"));
        personList.add(new Person(1, "a7"));
        personList.add(new Person(2, "b2"));
        personList.add(new Person(3, "c1"));
        personList.add(new Person(3, "c2"));
        KeyAffinityExecutor<Integer> executor = KeyAffinityExecutor.newSerializerExecutor(10, "test-thread-%d");
        personList.forEach(person -> {
            executor.executeAffinity(person.id, () -> {

                System.out.print(Thread.currentThread().getName());
                System.out.print("----");
                System.out.print(person.data);
                System.out.print("\n");
                //                System.out.println(Thread.currentThread().getName() + "----" + person.data);
            });
        });
    }

    private void testNormalExecutor() {
        List<Person> personList = new ArrayList<>();
        personList.add(new Person(1, "a1"));
        personList.add(new Person(2, "b1"));
        personList.add(new Person(1, "a2"));
        personList.add(new Person(2, "b2"));
        personList.add(new Person(3, "c1"));
        personList.add(new Person(3, "c2"));
        System.out.println("普通线程池");
        ExecutorService service = Executors.newFixedThreadPool(10);
        personList.forEach(person -> {
            service.execute(() -> {
                System.out.print(Thread.currentThread().getName());
                System.out.print("----");
                System.out.print(person.data);
                System.out.print("\n");
            });
        });
    }

    private void testExecutor() {

        List<Person> personList = new ArrayList<>();
        personList.add(new Person(1, "a1"));
        personList.add(new Person(2, "b1"));
        personList.add(new Person(1, "a2"));
        personList.add(new Person(2, "b2"));
        personList.add(new Person(3, "c1"));
        personList.add(new Person(3, "c2"));

        ExecutorService[] services = new ExecutorService[10];
        IntStream.range(0, 9)
                .boxed()
                .forEach(i -> services[i] = Executors.newSingleThreadExecutor());
        personList.forEach(person -> {
            services[person.id % 10].execute(() -> {
                System.out.println(Thread.currentThread().getName() + "----" + person.data);
            });
        });
    }


    static class Person {
        private Integer id;
        private String data;

        Person(Integer id, String data) {
            this.id = id;
            this.data = data;
        }

        @Override
        public String toString() {
            return reflectionToString(this, SHORT_PREFIX_STYLE);
        }
    }
}