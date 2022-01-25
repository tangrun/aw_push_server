/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2024. All rights reserved.
 */
package com.huawei.push.util;

import com.huawei.push.messaging.HuaweiApp;
import com.huawei.push.messaging.HuaweiCredential;
import com.huawei.push.messaging.HuaweiOption;

import java.util.ResourceBundle;

public class InitAppUtils {
    /**
     * @return HuaweiApp
     * @deprecated 请调用带参初始化方法
     */
    public static HuaweiApp initializeApp() {
        if (true) throw new RuntimeException("");
        String appId = ResourceBundle.getBundle("url").getString("appid");
        String appSecret = ResourceBundle.getBundle("url").getString("appsecret");
        // Create HuaweiCredential
        // This appId and appSecret come from Huawei Developer Alliance
        return initializeApp(appId, appSecret);
    }

    private static HuaweiApp initializeApp(String appId, String appSecret) {
        HuaweiCredential credential = HuaweiCredential.builder()
                .setAppId(appId)
                .setAppSecret(appSecret)
                .build();

        // Create HuaweiOption
        HuaweiOption option = HuaweiOption.builder()
                .setCredential(credential)
                .build();

        // Initialize HuaweiApp
//        return HuaweiApp.initializeApp(option);
        return HuaweiApp.getInstance(option);
    }
}
