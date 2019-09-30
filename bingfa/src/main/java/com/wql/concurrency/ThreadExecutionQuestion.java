package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

/**
 * 如何保证t1 t2 t3 三个线程按照1，2，3的方式执行
 */
public class ThreadExecutionQuestion {

    public static void main(String[] args) throws Exception {

        threadSleep();

    }

    private static void threadWait() {
        Thread t1 = new Thread(ThreadExecutionQuestion::access, "t1");
        Thread t2 = new Thread(ThreadExecutionQuestion::access, "t2");
        Thread t3 = new Thread(ThreadExecutionQuestion::access, "t3");

        threadStartAndWait(t1);
        threadStartAndWait(t2);
        threadStartAndWait(t3);
    }

    private static void threadStartAndWait(Thread thread) {

        if (Thread.State.NEW.equals(thread.getState())) {
            thread.start();
        }

        while (thread.isAlive()) {
            synchronized(thread) {
                try {
                    thread.wait(); //到底是谁通知了 Thread -》 notify
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    private static void threadLoop() throws Exception {
        Thread t1 = new Thread(ThreadExecutionQuestion::access, "t1");
        Thread t2 = new Thread(ThreadExecutionQuestion::access, "t2");
        Thread t3 = new Thread(ThreadExecutionQuestion::access, "t3");

        t1.start();
        while (t1.isAlive()) {
            //自旋
        }

        t2.start();
        while (t2.isAlive()) {

        }

        t3.start();
        while (t3.isAlive()) {

        }
    }

    private static void threadSleep() throws Exception {
        Thread t1 = new Thread(ThreadExecutionQuestion::access, "t1");
        Thread t2 = new Thread(ThreadExecutionQuestion::access, "t2");
        Thread t3 = new Thread(ThreadExecutionQuestion::access, "t3");

        t1.start();
        while (t1.isAlive()) {
            // sleep
            Thread.sleep(0);
        }

        t2.start();
        while (t2.isAlive()) {
            Thread.sleep(0);

        }

        t3.start();
        while (t3.isAlive()) {
            Thread.sleep(0);

        }
    }

    private static void threadJoinOneByOne() throws Exception {
        Thread t1 = new Thread(ThreadExecutionQuestion::access, "t1");
        Thread t2 = new Thread(ThreadExecutionQuestion::access, "t2");
        Thread t3 = new Thread(ThreadExecutionQuestion::access, "t3");

        // start() 仅仅是通知线程启动
        t1.start();
        // join() 控制线程必须执行完成
        t1.join();

        t2.start();
        t2.join();

        t3.start();
        t3.join();
    }

    private static void access() {
        System.out.printf("[%s] 线程正在执行....\n", Thread.currentThread().getName());
    }
}
