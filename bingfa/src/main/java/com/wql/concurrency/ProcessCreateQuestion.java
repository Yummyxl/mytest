package com.wql.concurrency;

import java.io.IOException;

/**
 * 应用模块名称<p>
 * 代码描述<p>
 *
 * @Author: Yummyxl
 * @Date: 2019-09-30
 */

public class ProcessCreateQuestion {

    public static void main(String[] args) {

        // 获取javaruntime
        Runtime runtime = Runtime.getRuntime();

        try {
            Process exec = runtime.exec("sublime text");
            exec.exitValue();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
