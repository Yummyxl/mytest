package com.wql.locks.grpcpool2;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-06-18
 */

public enum MyConnectivityState {

    OK,
    CANCEL,
    DOWN;

    private MyConnectivityState() {
    }
}
