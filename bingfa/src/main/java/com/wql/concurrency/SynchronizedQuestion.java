package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

public class SynchronizedQuestion {

    Object o1 = new Object();
    static Object o2 = new Object();

    public synchronized static void test1() {}

    public synchronized void test2() {}

    public void test3() {
        synchronized (o1) {

        }
    }

    public void test4() {
        synchronized (o2) {

        }
    }

    public void test5() {
        synchronized (new Object()) {

        }
    }
}
