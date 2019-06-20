package com.wql.locks.grpcpool2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-19
 */

public class ConsulGrpcPool {
    private static Logger logger = LoggerFactory.getLogger(ConsulGrpcPool.class);
    private final String serviceName;
    private long checkInterval = 2000L;
    private String consulIp = "localhost";
    private int port = 8500;
    private GrpcChannelPoolConfig grpcChannelPoolConfig;
    private final List<String> instances = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private GrpcChannelFixedPool grpcChannelFixedPool;
    private final ScheduledExecutorService scheduledExecutorService;
    private final String consulHealthInstaceUrl = "http://hostIp:port/v1/catalog/service/myServiceName?passing";
    private volatile boolean healthFlag = true;

    public ConsulGrpcPool(String serviceName, String consulIp, int port, ScheduledExecutorService scheduledExecutorService) {
        this.serviceName = serviceName;
        this.consulIp = consulIp;
        this.port = port;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ConsulGrpcPool(String serviceName, ScheduledExecutorService scheduledExecutorService) {
        this.serviceName = serviceName;
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ConsulGrpcPool(ConsulGrpcPoolConfig consulGrpcPoolConfig) {
        this.serviceName = consulGrpcPoolConfig.getServiceName();
        if (consulGrpcPoolConfig.getCheckInterval() > 0 && consulGrpcPoolConfig.getCheckInterval() < 10000L) {
            checkInterval = consulGrpcPoolConfig.getCheckInterval();
        }
        this.port = consulGrpcPoolConfig.getPort();
        this.consulIp = consulGrpcPoolConfig.getConsulIp();

        this.grpcChannelPoolConfig = GrpcChannelPoolConfig.builder().checkReadyTimeout(consulGrpcPoolConfig.getCheckReadyTimeout())
                .connecTimeOut(consulGrpcPoolConfig.getConnecTimeOut())
                .expireTimeOut(consulGrpcPoolConfig.getExpireTimeOut())
                .heartbeatInterval(consulGrpcPoolConfig.getHeartbeatInterval())
                .maxCount(consulGrpcPoolConfig.getMaxCount())
                .retry(consulGrpcPoolConfig.getRetry()).build();
        this.scheduledExecutorService = consulGrpcPoolConfig.getScheduledExecutorService();
    }

    public ManagedChannel getGrpcChannelFromConsulGrpcChannelPool() throws Exception{
        if (!healthFlag) {
            throw new Exception("service name 为 " + serviceName + " 没有可用服务");
        }
        if (grpcChannelFixedPool == null) {
            lock.lock();
            try {
                if (grpcChannelFixedPool == null) {
                    if (instances.size() == 0) {
                        List<String> aliveAddressOfInstances = getAliveAddressOfInstances();
                        if (aliveAddressOfInstances == null) {
                            throw new Exception("consul client " + consulIp + " 不健康");
                        }
                        if (aliveAddressOfInstances.size() == 0) {
                            healthFlag = false;
                            throw new Exception("service name 为 " + serviceName + " 没有可用服务");
                        }
                        instances.addAll(getAliveAddressOfInstances());
                    }
                    if (grpcChannelPoolConfig == null) {
                        grpcChannelFixedPool = new GrpcChannelFixedPool(instances.stream().collect(Collectors.joining(",")));
                    } else {
                        grpcChannelFixedPool = new GrpcChannelFixedPool(instances.stream().collect(Collectors.joining(",")), grpcChannelPoolConfig);
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return grpcChannelFixedPool.getConnectionFromFixedPool();
    }

    private void checkLiveAddress() {
        List<String> alives = getAliveAddressOfInstances();
        if (alives == null) {
            return;
        }
        if (alives.size() == 0) {
            healthFlag = false;
            return;
        }
        List<String> removes = new ArrayList<>();
        boolean flag = false;
        for (int i = 0; i < instances.size(); i++) {
            if (!alives.contains(instances.get(i))) {
                flag = true;
                removes.add(instances.get(i));
            }
        }
        if (flag || instances.size() == 0) {
            instances.clear();
            instances.addAll(alives);
            fixAddress(alives, removes);
        }
    }

    private void fixAddress(List<String> lives, List<String> remove) {
        lock.lock();
        try {
            if (grpcChannelFixedPool == null) {
                if (grpcChannelPoolConfig == null) {
                    grpcChannelFixedPool = new GrpcChannelFixedPool(instances.stream().collect(Collectors.joining(",")));
                } else {
                    grpcChannelFixedPool = new GrpcChannelFixedPool(instances.stream().collect(Collectors.joining(",")), grpcChannelPoolConfig);
                }
            }
            grpcChannelFixedPool.delUnalivePool(lives, remove);
        } finally {
            lock.unlock();
        }
    }

    private List<String> getAliveAddressOfInstances() {
        String url = consulHealthInstaceUrl.replace("hostIp", consulIp)
                .replace("port", "" + port)
                .replace("myServiceName", serviceName);
        try {
            HttpResponse<String> response = Unirest.get(url).asString();
            logger.debug("consul node return: " + response.getBody());
            JsonParser jsonParser = new JsonParser();
            JsonArray asJsonArray = jsonParser.parse(response.getBody()).getAsJsonArray();
            List<String> res = new ArrayList<>();
            for (int i=0; i<asJsonArray.size(); i++) {
                JsonObject asJsonObject = asJsonArray.get(i).getAsJsonObject();
                res.add(asJsonObject.get("ServiceAddress").getAsString() + ":" + asJsonObject.get("ServicePort").getAsInt());
            }
            return res;
        } catch (Exception e) {
            logger.error("consul client " + consulIp + " 不健康");
            return null;
        }
    }

    private boolean checkString(String str) {
        return (str != null && str.trim().length() > 0);
    }

    public void start() {
        scheduledExecutorService.scheduleAtFixedRate(()->checkLiveAddress(), 0, checkInterval, TimeUnit.MILLISECONDS);
    }
}
