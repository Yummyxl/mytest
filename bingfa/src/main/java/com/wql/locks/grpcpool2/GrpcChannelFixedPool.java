package com.wql.locks.grpcpool2;

import io.grpc.ManagedChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-19
 */

public class GrpcChannelFixedPool {

    private List<String> addsList;
    private final Map<String, GrpcChannelPool> poolMap = new HashMap<>();
    private final GrpcChannelPoolConfig grpcChannelPoolConfig;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public GrpcChannelFixedPool(String adds) {
        String[] split = adds.split(",");
        this.addsList = new ArrayList<>();
        for (String s : split) {
            addsList.add(s);
        }
        grpcChannelPoolConfig = null;
    }

    public GrpcChannelFixedPool(String adds, GrpcChannelPoolConfig grpcChannelPoolConfig) {
        String[] split = adds.split(",");
        this.addsList = new ArrayList<>();
        for (String s : split) {
            this.addsList.add(s);
        }
        this.grpcChannelPoolConfig = grpcChannelPoolConfig;
    }

    public ManagedChannel getConnectionFromFixedPool() throws Exception {
        String hostIpAndPort = getHostIpAndPort();
        GrpcChannelPool grpcChannelPool = null;
        if (poolMap.containsKey(hostIpAndPort)) {
            lock.readLock().lock();
            try {
                grpcChannelPool = poolMap.get(hostIpAndPort);
            } finally {
                lock.readLock().unlock();
            }
        } else {
            lock.writeLock().lock();
            boolean flag = true;
            try {
                if (poolMap.containsKey(hostIpAndPort)) {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                    flag = false;
                    try {
                        grpcChannelPool = poolMap.get(hostIpAndPort);
                    } finally {
                        lock.readLock().unlock();
                    }
                } else {
                    grpcChannelPool = new GrpcChannelPool(hostIpAndPort, grpcChannelPoolConfig);
                    poolMap.put(hostIpAndPort, grpcChannelPool);
                }
            } finally {
                if (flag) {
                    lock.writeLock().unlock();
                }
            }
        }
        return grpcChannelPool.getGrpcChannelFromPool();
    }


    private String getHostIpAndPort() {
        lock.readLock().lock();
        try {
            return addsList.get(ThreadLocalRandom.current().nextInt(addsList.size()) & addsList.size());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void delUnalivePool(List<String> alives, List<String> removes) {
        lock.writeLock().lock();
        try {
            addsList.clear();
            addsList.addAll(alives);
            removes.forEach(a -> {
                if (poolMap.containsKey(a)) {
                    GrpcChannelPool grpcChannelPool = poolMap.get(a);
                    grpcChannelPool.clear();
                }
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
}
