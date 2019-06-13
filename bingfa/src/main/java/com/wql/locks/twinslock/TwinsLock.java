package com.wql.locks.twinslock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * 应用模块名称<lock>
 * 代码描述<一次最多只有两个线程可以执行>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-13
 */

public class TwinsLock implements Lock {

    @Override
    public void lock() {
        
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        return null;
    }
}
