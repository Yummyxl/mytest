package com.wql.locks.grpcpool;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-14
 */

public class GrpcChannel {

    private String hostIp;
    private int port;
    private ManagedChannel channel;
    private ConnectivityState state;
    private GrpcChannelPool grpcChannelPool;
    private long expire;
    private int retry;
    private long heartbeatInterval;
    private final Lock lock = new ReentrantLock();

    public GrpcChannel(String adderss, GrpcChannelPool grpcChannelPool) {
        String[] split = adderss.split(":");
        this.hostIp = split[0];
        this.port = Integer.valueOf(split[1]);
        this.grpcChannelPool = grpcChannelPool;
        this.heartbeatInterval = grpcChannelPool.getHeartbeatInterval();
    }

    public ChannelStateEnum tryConnection(boolean force) {
        lock.lock();
        try {
            if (!force && channel != null) {
                if (grpcChannelPool.checkStateOfConn(channel)) {
                    return ChannelStateEnum.SUCCESS;
                } else {
                    return ChannelStateEnum.ERROR;
                }
            }
            if (channel != null) {
                channel.shutdownNow();
            }
            channel = grpcChannelPool.makeChannel(hostIp, port);
            ConnectivityState connectivityState = grpcChannelPool.readyCheck(this);
            if (!ConnectivityState.READY.equals(connectivityState)) {
                channel.shutdownNow();
                return ChannelStateEnum.ERROR;
            }
            new HeartBeat(this);
            ready();
            return ChannelStateEnum.SUCCESS;
        } finally {
            lock.unlock();
        }
    }

    void heartBeat(){
        switch (this.state) {
            case SHUTDOWN:
                shutDown();
                break;
            default:
                healthCheck();
                break;
        }
    }

    private void healthCheck(){
        lock.lock();
        try {
            switch (grpcChannelPool.readyCheck(this)) {
                case READY:
                    ready();
                    break;
                case SHUTDOWN:
                    shutDown();
                    break;
                default:
                    if (expire < System.currentTimeMillis()) {
                        shutDown();
                    } else {
                        idle();
                    }
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    private void ready() {
        this.state = ConnectivityState.READY;
        this.expire = System.currentTimeMillis() + this.grpcChannelPool.getTimeOut();
        this.retry = 0;
        this.grpcChannelPool.connReady(this.hostIp + ":" + this.port, this);
    }

    public void shutDown() {
        lock.lock();
        try {
            this.state = ConnectivityState.SHUTDOWN;
            channel.shutdownNow();
            grpcChannelPool.connUnReady(this.hostIp + ":" + this.port);
        } finally {
            lock.unlock();
        }
    }

    private void idle() {
        this.state = ConnectivityState.IDLE;
        retry++;
        if (retry > grpcChannelPool.getRetry()) {
            shutDown();
            return;
        }
        grpcChannelPool.connUnReady(this.hostIp + ":" + this.port);
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public void setChannel(ManagedChannel channel) {
        this.channel = channel;
    }

    public ConnectivityState getState() {
        return state;
    }

    public void setState(ConnectivityState state) {
        this.state = state;
    }

    public GrpcChannelPool getGrpcChannelPool() {
        return grpcChannelPool;
    }

    public void setGrpcChannelPool(GrpcChannelPool grpcChannelPool) {
        this.grpcChannelPool = grpcChannelPool;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public Lock getLock() {
        return lock;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }
}
