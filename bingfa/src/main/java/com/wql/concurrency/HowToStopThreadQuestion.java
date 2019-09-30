package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

public class HowToStopThreadQuestion {

    public static void main(String[] args) throws Exception {

        Action action = new Action();
        Thread t1 = new Thread(action, "t1");
        t1.start();
        action.setStoped(true);
        t1.join();
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
