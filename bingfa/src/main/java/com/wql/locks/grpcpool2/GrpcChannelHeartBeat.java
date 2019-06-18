package com.wql.locks.grpcpool2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-18
 */

public class GrpcChannelHeartBeat extends Thread {

    private static Logger logger = LoggerFactory.getLogger(GrpcChannelHeartBeat.class);
    private GrpcChannel grpcChannel;

    GrpcChannelHeartBeat(GrpcChannel grpcChannel) {
        this.grpcChannel = grpcChannel;
    }

    @Override
    public void run() {
        while (grpcChannel.getChannel() != null && grpcChannel.getState() != MyConnectivityState.DOWN) {
            try {
                logger.debug(Thread.currentThread().getName() + " 健康健康检查开始，目标地址为:" + grpcChannel.getGrpcChannelPool().getHostIpAndPort());
                grpcChannel.heartBeat();
                TimeUnit.MILLISECONDS.sleep(grpcChannel.getGrpcChannelPool().getHeartbeatInterval());
            } catch (InterruptedException e) {
                logger.error(Thread.currentThread().getName() + " 健康健康检查异常，目标地址为:" + grpcChannel.getGrpcChannelPool().getHostIpAndPort());
                grpcChannel.shutDown();
                break;
            }
        }
    }
}
