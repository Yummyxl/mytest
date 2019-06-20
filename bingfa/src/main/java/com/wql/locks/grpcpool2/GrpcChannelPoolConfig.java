package com.wql.locks.grpcpool2;

import lombok.Builder;
import lombok.Getter;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-19
 */

@Builder
@Getter
public class GrpcChannelPoolConfig {

    private long connecTimeOut = 3000L;
    private long expireTimeOut = 20000L;
    private long checkReadyTimeout = 300L;
    private long heartbeatInterval = 10000L;
    private int retry = 5;
    private int maxCount = 8;

}
