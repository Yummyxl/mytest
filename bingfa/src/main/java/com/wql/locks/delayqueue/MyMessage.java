package com.wql.locks.delayqueue;

import lombok.Data;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-07-03
 */

@Data
public class MyMessage implements Delayed {

    private int id;
    private String body;
    private long time;

    public MyMessage(int id, String body, long time) {
        this.id = id;
        this.body = body;
        this.time = TimeUnit.NANOSECONDS.convert(time, TimeUnit.MILLISECONDS) + System.nanoTime();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o == this) {
            return 0;
        }
        if (o instanceof MyMessage) {
            MyMessage m = (MyMessage) o;
            long diff = time - m.getTime();
            if (diff < 0) {
                return -1;
            } else if (diff > 0) {
                return 1;
            } else if (id - m.getId() < 0) {
                return -1;
            } else {
                return 1;
            }
        }
        long d = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
        return d == 0 ? 0 : d < 0 ? -1 : 1;
    }
}