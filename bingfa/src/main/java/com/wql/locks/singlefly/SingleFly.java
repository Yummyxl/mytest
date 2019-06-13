package com.wql.locks.singlefly;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Auther: Yummyxl
 * @Date: 2019/5/22 10:12
 */
public class SingleFly {

    private static final ConcurrentMap<String, Call> calls = new ConcurrentHashMap<>();

    private String functionName;

    public SingleFly(String functionName) {
        this.functionName = functionName;
    }

    @SuppressWarnings("unchecked")
    public <V> V execute(Callable<V> callable) throws Exception {
        Call<V> call = calls.get(functionName);
        if (call == null) {
            call = new Call<>();
            Call<V> other = calls.putIfAbsent(functionName, call);
            if (other == null) {
                try {
                    return call.exec(callable);
                } finally {
                    calls.remove(functionName);
                }
            } else {
                call = other;
            }
        }
        return call.await();
    }

    private static class Call<V> {
        private final Object lock = new Object();
        private boolean finished;
        private V result;
        private Exception exc;

        void finished(V result, Exception exc) {
            synchronized (lock) {
                this.finished = true;
                this.result = result;
                this.exc = exc;
                lock.notifyAll();
            }
        }

        V await() throws Exception {
            synchronized (lock) {
                while (!finished) {
                    lock.wait();
                }
                if (exc != null) {
                    throw exc;
                }
                return result;
            }
        }

        V exec(Callable<V> callable) throws Exception {
            V result = null;
            Exception exc = null;
            try {
                result = callable.call();
                return result;
            } catch (Exception e) {
                exc = e;
                throw e;
            } finally {
                finished(result, exc);
            }
        }
    }
}
