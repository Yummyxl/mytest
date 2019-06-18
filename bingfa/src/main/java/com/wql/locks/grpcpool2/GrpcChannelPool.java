package com.wql.locks.grpcpool2;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-18
 */

public class GrpcChannelPool {
    private static Logger logger = LoggerFactory.getLogger(GrpcChannel.class);
    private final String hostIpAndPort;
    private long connecTimeOut = 3000L;
    private long expireTimeOut = 20000L;
    private long checkReadyTimeout = 30L;
    private long heartbeatInterval = 10000L;
    private int retry = 5;
    private int maxCount = 8;
    private volatile int currentCount;
    private CopyOnWriteArrayList<GrpcChannel> grpcChannels = new CopyOnWriteArrayList<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public GrpcChannelPool(String hostIpAndPort) {
        this.hostIpAndPort = hostIpAndPort;
    }

    boolean readyCheck(ManagedChannel channel) {
        return MyConnectivityState.OK.equals(tryReadyCheck(channel));
    }

    protected MyConnectivityState tryReadyCheck(ManagedChannel channel) {
        ConnectivityState states = channel.getState(true);
        switch (states) {
            case READY:
            case IDLE:
                return MyConnectivityState.OK;
            case SHUTDOWN:
                return MyConnectivityState.DOWN;
            default:
                return MyConnectivityState.CANCEL;
        }
    }

    ManagedChannel makeChannel() {
        return tryMakeChannel();
    }

    protected ManagedChannel tryMakeChannel() {
        String[] split = hostIpAndPort.split(":");
        return ManagedChannelBuilder.forAddress(split[0], Integer.valueOf(split[1])).usePlaintext().build();
    }

    public ManagedChannel getGrpcChannelFromPool() throws Exception{
        logger.debug(Thread.currentThread().getName() + " 开始获取grpc连接从连接池: " + hostIpAndPort);
        GrpcChannel grpcChannel = null;
        if (currentCount < maxCount) {
            lock.writeLock().lock();
            try {
                if (currentCount < maxCount) {
                    grpcChannel = new GrpcChannel(this);
                    grpcChannels.add(grpcChannel);
                    currentCount++;
                } else {
                    grpcChannel = getFromGrpcChannelsWithOutLock();
                }
            } finally {
                lock.writeLock().unlock();
            }
        } else {
            if (grpcChannel == null) {
                grpcChannel = getFromGrpcChannels();
            }
        }
        long theTimeOut = System.currentTimeMillis() + connecTimeOut;
        ManagedChannel res;
        while (System.currentTimeMillis() < theTimeOut) {
            res = grpcChannel.tryConn();
            if (null != res) {
                logger.debug(Thread.currentThread().getName() + " 成功获取grpc连接从连接池: " + hostIpAndPort);
                return res;
            }
        }
        throw new Exception(Thread.currentThread().getName() +  " 获取grpc连接从连接池超时" + hostIpAndPort);
    }

    private GrpcChannel getFromGrpcChannels() {
        lock.readLock().lock();
        try {
            return grpcChannels.get(ThreadLocalRandom.current().nextInt(grpcChannels.size()) & grpcChannels.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    private GrpcChannel getFromGrpcChannelsWithOutLock() {
        return grpcChannels.get(ThreadLocalRandom.current().nextInt(grpcChannels.size()) & grpcChannels.size());
    }

    void shutDownGrpcChannel(GrpcChannel grpcChannel) {
        lock.writeLock().lock();
        try {
            if (grpcChannels.remove(grpcChannel)) {
                currentCount--;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    void ok(GrpcChannel grpcChannel) {
        lock.writeLock().lock();
        try {
            if (!grpcChannels.contains(grpcChannel)) {
                if (currentCount < maxCount) {
                    grpcChannels.add(grpcChannel);
                    currentCount++;
                } else {
                    grpcChannel.shutDown();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    String getHostIpAndPort() {
        return hostIpAndPort;
    }

    long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    long getExpireTimeOut() {
        return expireTimeOut;
    }

    int getRetry() {
        return retry;
    }
}
