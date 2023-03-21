package com.example.demo.service;


import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.HashMap;
import java.util.Map;

public class KeepAlive extends Thread {
    @Override
    public void run() {
        try {
            while (true) {
                //Send a heart per 22 seconds.
                Thread.sleep(22000);
                Map keepAliveParamMap = new HashMap<>(1);
                keepAliveParamMap.put("token", Login.TOKEN_VALUE);
                JSONObject heartResponse = JSONObject.parseObject(Login.sendPostOrPut(Login.KEEP_ALIVE_URL, keepAliveParamMap, Login.PUT));
//                if (Login.SUCCESS_CODE.equals(heartResponse.getString("code"))) {
//                    System.out.println("heart success!");
//                }

                Login.HEART_COUNT++;
                //Update the token per 22 minutes.
                if (Login.HEART_COUNT % 60 == 0) {
                    //Restore the heartCount to zero.
                    Login.HEART_COUNT = 0;
                    String signatureForUpdataToken = DigestUtils.md5Hex(Login.SIGNATURE_MD5_TEMP4 + ":" + Login.TOKEN_VALUE);
                    Map updateTokenParamMap = new HashMap<>(2);
                    updateTokenParamMap.put("token", Login.TOKEN_VALUE);
                    updateTokenParamMap.put("signature", signatureForUpdataToken);
                    JSONObject udpateTokenResponse = JSONObject.parseObject(Login.sendPostOrPut(Login.UPDATE_TOKEN_URL, updateTokenParamMap, Login.POST));
                    if (Login.SUCCESS_CODE.equals(udpateTokenResponse.getString("code"))) {
                        String newTokenValue = udpateTokenResponse.getJSONObject("data").getString(Login.TOKEN);
//                        System.out.println(String.format("update token success! newtoken:%s", newTokenValue));
                        Login.TOKEN_VALUE = newTokenValue;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Loi lay token");
        }

    }
}
