package com.wql.locks.grpcpool2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
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
    private long checkReadyTimeout = 300L;
    private long heartbeatInterval = 10000L;
    private int retry = 5;
    private int maxCount = 4;
    private volatile int currentCount;
    private CopyOnWriteArrayList<GrpcChannel> grpcChannels = new CopyOnWriteArrayList<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public GrpcChannelPool(String hostIpAndPort, GrpcChannelPoolConfig grpcChannelPoolConfig) {
        this.hostIpAndPort = hostIpAndPort;
        if (grpcChannelPoolConfig != null) {
            if (grpcChannelPoolConfig.getMaxCount() > 0 && grpcChannelPoolConfig.getMaxCount() <= 16) {
                this.maxCount = grpcChannelPoolConfig.getMaxCount();
            }
            if (grpcChannelPoolConfig.getConnecTimeOut() > 0 && grpcChannelPoolConfig.getConnecTimeOut() <= 20000L) {
                this.connecTimeOut = grpcChannelPoolConfig.getConnecTimeOut();
            }
            if (grpcChannelPoolConfig.getExpireTimeOut() > 0 && grpcChannelPoolConfig.getExpireTimeOut() <= 100000L) {
                this.expireTimeOut = grpcChannelPoolConfig.getExpireTimeOut();
            }
            if (grpcChannelPoolConfig.getCheckReadyTimeout() > 0 && grpcChannelPoolConfig.getCheckReadyTimeout() <= 5000L) {
                this.checkReadyTimeout = grpcChannelPoolConfig.getCheckReadyTimeout();
            }
            if (grpcChannelPoolConfig.getHeartbeatInterval() > 0 && grpcChannelPoolConfig.getHeartbeatInterval() <= 200000L) {
                this.heartbeatInterval = grpcChannelPoolConfig.getHeartbeatInterval();
            }
            if (grpcChannelPoolConfig.getRetry() > 0 && grpcChannelPoolConfig.getRetry() <= 10) {
                this.retry = grpcChannelPoolConfig.getRetry();
            }
        }
    }

    boolean readyCheck(ManagedChannel channel) {
        return MyConnectivityState.OK.equals(tryReadyCheck(channel));
    }

    protected MyConnectivityState tryReadyCheck(ManagedChannel channel) {
        try {
            HealthGrpc.HealthBlockingStub healthBlockingStub = HealthGrpc.newBlockingStub(channel).withDeadlineAfter(checkReadyTimeout, TimeUnit.MILLISECONDS);
            HealthCheckRequest build = HealthCheckRequest.newBuilder().setService("").build();
            HealthCheckResponse check = healthBlockingStub.check(build);
            HealthCheckResponse.ServingStatus status = check.getStatus();
            if (HealthCheckResponse.ServingStatus.SERVING.equals(status)) {
                return MyConnectivityState.OK;
            }
            return MyConnectivityState.DOWN;
        } catch (Exception e) {
            logger.error("连接不可用");
            return MyConnectivityState.DOWN;
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
            boolean flag = true;
            lock.writeLock().lock();
            try {
                if (currentCount < maxCount) {
                    grpcChannel = new GrpcChannel(this);
                    grpcChannels.add(grpcChannel);
                    currentCount++;
                    flag = false;
                } else {
                    lock.writeLock().unlock();
                    grpcChannel = getFromGrpcChannels();
                }
            } finally {
                if (!flag) {
                    lock.writeLock().unlock();
                }
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

    public void destroyAllGrpcChannels() {
        lock.writeLock().lock();
        try {
            Iterator<GrpcChannel> iterator = grpcChannels.iterator();
            while (iterator.hasNext()) {
                GrpcChannel next = iterator.next();
                next.shutDown();
                iterator.remove();
            }
            grpcChannels = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private GrpcChannel getFromGrpcChannels() {
        lock.readLock().lock();
        try {
            return grpcChannels.get(ThreadLocalRandom.current().nextInt(grpcChannels.size()) & grpcChannels.size());
        } finally {
            lock.readLock().unlock();
        }
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

    void clear() {
        lock.writeLock().lock();
        try {
            grpcChannels.forEach(a -> a.shutDown());
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
