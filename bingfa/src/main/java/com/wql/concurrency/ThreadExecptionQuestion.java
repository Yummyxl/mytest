package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */


public class ThreadExecptionQuestion {

    public static void main(String[] args) throws InterruptedException {

        // 注册
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.printf("%s 线程 遇到了 %s 问题", thread.getName(), throwable.getMessage());
        });

        Thread t1 = new Thread(() -> {
            throw new RuntimeException("数据拿到");
        }, "t1");
        t1.start();
        t1.join();

        // java 中线程只是一个包装，由gc来做回收
        // jvm thread 对应一个os thread，由jvm管理
        // 当线程执行完毕（执行完或者异常），所以异常也是一种中断线程当方法
        System.out.println(t1.isAlive());
    }
}
