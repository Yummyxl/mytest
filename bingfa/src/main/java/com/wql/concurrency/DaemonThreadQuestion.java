package com.wql.concurrency;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

public class DaemonThreadQuestion {

    public static void main(String[] args) {

        //main 线程
        Thread t1 = new Thread(() -> {
            System.out.println("hello");
        }, "t1");
        t1.setDaemon(true);

        // 守护进程的执行依赖于执行时间，也并不是唯一评判
        t1.start();
    }
}
