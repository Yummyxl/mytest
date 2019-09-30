package com.wql.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

public class ThreadPoolExecutorExecptionQuestion {

    public static void main(String[] args) throws InterruptedException {

//        ExecutorService executorService = Executors.newFixedThreadPool(2);

        ExecutorService executorService = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>()
        ) {
            /**
             * 通过覆盖方法 {@link ThreadPoolExecutor#afterExecute(Runnable, Throwable)} 达到获取异常信息
             * @param r
             * @param t
             */
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                System.out.printf("%s 线程 遇到了 %s 问题", Thread.currentThread().getName(), t.getMessage());
            }
        };

        executorService.execute(() -> {
            throw new RuntimeException("数据不对！！！");
        });

        executorService.awaitTermination(1, TimeUnit.SECONDS);
        // 关闭线程池
        executorService.shutdown();

    }
}
