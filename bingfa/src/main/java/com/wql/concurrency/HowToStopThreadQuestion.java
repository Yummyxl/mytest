package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

/**
 * public void interrupt() 将调用者线程的中断状态设为true。
 * public boolean isInterrupted() 判断调用者线程的中断状态。
 * public static boolean interrupted 只能通过Thread.interrupted()调用。
 * 它会做两步操作：返回当前线程的中断状态；将当前线程的中断状态设为false；
 *
 */
public class HowToStopThreadQuestion {

    public static void main(String[] args) throws Exception {

        Action action = new Action();
        Thread t1 = new Thread(action, "t1");
        t1.start();
        action.setStoped(true);
        t1.join();

        Thread t2 = new Thread(() -> {
            if (!Thread.currentThread().isInterrupted()) {
                access();
            }
        }, "t2");
        t2.start();
        // 中断操作，仅仅是设置状态，而非中断线程
        t2.interrupt();
    }

    public static class Action implements Runnable {

        // 线程安全问题，确保可见性 (happen-before)
        private volatile boolean stoped = false;

        @Override
        public void run() {
            if (!stoped) {
                access();
            }
        }

        public void setStoped(boolean stoped) {
            this.stoped = stoped;
        }
    }

    private static void access() {
        System.out.printf("[%s] 线程正在执行....\n", Thread.currentThread().getName());
    }
}
