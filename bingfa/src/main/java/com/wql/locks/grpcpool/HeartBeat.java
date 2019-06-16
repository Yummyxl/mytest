package com.wql.locks.grpcpool;

import io.grpc.ConnectivityState;

import java.util.concurrent.TimeUnit;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-16
 */

public class HeartBeat extends Thread {

    private GrpcChannel channel;

    public HeartBeat(GrpcChannel channel) {
        this.channel = channel;
    }

    @Override
    public void run() {
        while (!ConnectivityState.SHUTDOWN.equals(channel.getState())) {
            try {
                channel.heartBeat();
                TimeUnit.MILLISECONDS.sleep(channel.getHeartbeatInterval());
            } catch (Exception e) {
                //todo log
                e.printStackTrace();
                channel.shutDown();
                break;
            }
        }
    }
}
