package com.wql.locks.grpcpool2;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-18
 */

public class GrpcChannel {
    private static Logger logger = LoggerFactory.getLogger(GrpcChannel.class);
    private ManagedChannel channel;
    private volatile MyConnectivityState state;
    private final GrpcChannelPool grpcChannelPool;
    private long expire;
    private int retryCount;
    private final Lock lock = new ReentrantLock();

    GrpcChannel(GrpcChannelPool grpcChannelPool) {
        this.grpcChannelPool = grpcChannelPool;
    }

    public ManagedChannel tryConn() {
        lock.lock();
        try {
            if (channel != null && checkStatus()) {
                return channel;
            }
            if (channel != null) {
                shutDown();
            }
            logger.debug("创建新的grpc channel:" + getGrpcChannelPool().getHostIpAndPort());
            channel = grpcChannelPool.makeChannel();
            if (!grpcChannelPool.readyCheck(channel)) {
                logger.debug("创建的新的grpc channel不可用" + getGrpcChannelPool().getHostIpAndPort());
                return null;
            }
            logger.debug("创建的新的grpc channel可用" + getGrpcChannelPool().getHostIpAndPort());
            ok();
            new GrpcChannelHeartBeat(this).start();
            return channel;
        } finally {
            lock.unlock();
        }
    }

    private boolean checkStatus() {
        if (state != null && MyConnectivityState.OK.equals(state)) {
            return true;
        }
        return false;
    }

    void heartBeat() {
        MyConnectivityState myConnectivityState = grpcChannelPool.tryReadyCheck(channel);
        logger.debug("健康检测结果为： " + myConnectivityState);
        switch (myConnectivityState) {
            case OK:
                ok();
                break;
            case DOWN:
                shutDown();
                break;
            default:
                cancel();
                break;
        }
    }

    void ok() {
        lock.lock();
        try {
            state = MyConnectivityState.OK;
            expire = System.currentTimeMillis() + grpcChannelPool.getExpireTimeOut();
            retryCount = 0;
            getGrpcChannelPool().ok(this);
        } finally {
            lock.unlock();
        }
    }

    void shutDown() {
        lock.lock();
        try {
            channel.shutdownNow();
            channel = null;
            state = MyConnectivityState.DOWN;
            grpcChannelPool.shutDownGrpcChannel(this);
        } finally {
            lock.unlock();
        }
    }

    void cancel() {
        lock.lock();
        try {
            this.state = MyConnectivityState.CANCEL;
            retryCount++;
            logger.debug(retryCount + "次检测失败");
            if (expire < System.currentTimeMillis() || retryCount > grpcChannelPool.getRetry()) {
                shutDown();
                return;
            }
            grpcChannelPool.shutDownGrpcChannel(this);
        } finally {
            lock.unlock();
        }
    }

    ManagedChannel getChannel() {
        return channel;
    }

    MyConnectivityState getState() {
        return state;
    }

    void setState(MyConnectivityState state) {
        this.state = state;
    }

    GrpcChannelPool getGrpcChannelPool() {
        return grpcChannelPool;
    }
}
