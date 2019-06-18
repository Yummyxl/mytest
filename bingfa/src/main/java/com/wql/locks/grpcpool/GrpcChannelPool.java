package com.wql.locks.grpcpool;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-15
 */

public class GrpcChannelPool {

    private long timeOut = 100000L;
    private long checkReadyTimeout = 30L;
    private long heartbeatInterval = 20000L;
    private int retry = 5;
    private ConcurrentMap<String, GrpcChannel> conns = new ConcurrentHashMap<>();
    private ConcurrentMap<String, GrpcChannel> alives = new ConcurrentHashMap<>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    protected ConnectivityState tryReadyCheck(ManagedChannel channel) {
        ConnectivityState state = channel.getState(true);
        if (ConnectivityState.READY.equals(state) || ConnectivityState.SHUTDOWN.equals(state)) {
            return state;
        }
        try {
            if (channel.awaitTermination(checkReadyTimeout, TimeUnit.MILLISECONDS)) {
                return channel.getState(true);
            }
        } catch (InterruptedException e) {
            //todo log
            e.printStackTrace();
            return ConnectivityState.SHUTDOWN;
        }
        return ConnectivityState.IDLE;
    }

    private ConnectivityState changeGrpcChannelState(final GrpcChannel channel) {
        switch (tryReadyCheck(channel.getChannel())) {
            case SHUTDOWN:
                return ConnectivityState.SHUTDOWN;
            case READY:
            case IDLE:
                return ConnectivityState.READY;
            default:
                return ConnectivityState.IDLE;
        }
    }

    public ManagedChannel getChannelFromPool() {
        return getConn(getRandomAddress());
    }

    public void prepareAllChannel() {
        lock.writeLock().lock();
        try {
            for (String address : alives.keySet()) {
                GrpcChannel tem = new GrpcChannel(address, this);
                tem.tryConnection(true);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private ManagedChannel getConn(String address) {
        lock.writeLock().lock();
        try {
            GrpcChannel grpcChannel = conns.get(address);
            if (grpcChannel == null) {
                GrpcChannel tem = new GrpcChannel(address, this);
                conns.put(address, tem);
            }
        } finally {
            lock.writeLock().unlock();
        }
        try {
            return getChannel(address);
        } catch (Exception e) {
            // todo log
            e.printStackTrace();
            String[] split = address.split(":");
            String ip = split[0];
            Integer port = Integer.valueOf(split[1]);
            return makeChannel(ip, port);
        }
    }

    private ManagedChannel getChannel(String address) throws Exception{
        if (checkChannelState(alives.get(address).tryConnection(false))) {
            return alives.get(address).getChannel();
        } else {
            if (checkChannelState(alives.get(address).tryConnection(true))) {
                return alives.get(address).getChannel();
            }
            //todo log
            throw new Exception("地址:" + address + " 不可用");
        }

    }

    private boolean checkChannelState(ChannelStateEnum channelStateEnum) {
        return ChannelStateEnum.SUCCESS.equals(channelStateEnum);
    }

    public String getRandomAddress() {
        lock.readLock().unlock();
        try {
            List<String> aliveAddres = new ArrayList<>(alives.keySet());
            return aliveAddres.get(ThreadLocalRandom.current().nextInt(aliveAddres.size()) & aliveAddres.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    void connReady(String name, GrpcChannel channel) {
        lock.writeLock().lock();
        try {
            alives.put(name, channel);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void connUnReady(String name) {
        lock.writeLock().lock();
        try {
            alives.remove(name);
        } finally {
            lock.writeLock().unlock();
        }
    }

    ConnectivityState readyCheck(final GrpcChannel channel) {
        return changeGrpcChannelState(channel);
    }

    protected ManagedChannel tryMakeChannel(String hostIp, int port) {
        return ManagedChannelBuilder.forAddress(hostIp, port).usePlaintext().build();
    }

    ManagedChannel makeChannel(String hostIp, int port) {
        return tryMakeChannel(hostIp, port);
    }

    protected boolean checkStateOfConn(ManagedChannel channel) {
        return ConnectivityState.READY.equals(channel.getState(true));
    }

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public long getCheckReadyTimeout() {
        return checkReadyTimeout;
    }

    public void setCheckReadyTimeout(long checkReadyTimeout) {
        this.checkReadyTimeout = checkReadyTimeout;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public ConcurrentMap<String, GrpcChannel> getConns() {
        return conns;
    }

    public void setConns(ConcurrentMap<String, GrpcChannel> conns) {
        this.conns = conns;
    }

    public ConcurrentMap<String, GrpcChannel> getAlives() {
        return alives;
    }

    public void setAlives(ConcurrentMap<String, GrpcChannel> alives) {
        this.alives = alives;
    }
}
