package com.wql.locks.grpcpool2;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-20
 */


@Builder
@Getter
public class ConsulGrpcPoolConfig {

    private final String serviceName;
    private long checkInterval = 2000L;
    private final String consulIp = "localhost";
    private final int port = 8500;
    private final ScheduledExecutorService scheduledExecutorService;

    private long connecTimeOut = 3000L;
    private long expireTimeOut = 20000L;
    private long checkReadyTimeout = 300L;
    private long heartbeatInterval = 10000L;
    private int retry = 5;
    private int maxCount = 8;
}
