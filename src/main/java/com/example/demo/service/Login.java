package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.twilio.Twilio;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.example.demo.constants.Global.*;


public class Login {

//44313
    static final String AUTH_URL = URL_DSS+"brms/api/v1.0/accounts/authorize";
    static final String KEEP_ALIVE_URL = URL_DSS+"brms/api/v1.0/accounts/keepalive";
    static final String UPDATE_TOKEN_URL = URL_DSS+"brms/api/v1.0/accounts/updateToken";
    static final String SUCCESS_CODE = "1000";
    static String SIGNATURE_MD5_TEMP4 = null;
    static String TOKEN_VALUE = null;
    static final String TOKEN = "token";
    static final String POST = "post";
    static final String PUT = "put";
    static int HEART_COUNT = 0;
    
    static String  SECRETKEY_WITH_SA = null;
    static String  DURATION = null;
    static String  SECRET_VECTOR = null;
    static String  PRIVATE_KEY = null;
    static String  PUBLIC_KEY = null;
    static String  USER_ID = null;




    public Login() throws Exception {
        //Step1: Try to login first time.
        Map firstLoginParams = new HashMap<>(3);
        firstLoginParams.put("userName", USER_NAME);
        firstLoginParams.put("ipAddress", ADDRESS);
        firstLoginParams.put("clientType", "WINPC_V2");

        String firstResponseString = sendPostOrPut(AUTH_URL, firstLoginParams,
                POST);

        JSONObject firstLoginResponse = JSONObject.parseObject(firstResponseString);
        //Step2: Try to login second time.
        String userName = USER_NAME;
        String passWord = PASS;
        String realm = firstLoginResponse.getString("realm");
        String randomKey = firstLoginResponse.getString("randomKey");
        String signature = generateSignature(userName, passWord, realm, randomKey);
        //Generate the public key. You can generate it in your own way, or this method we used.
        KeyPair keyPair = getRsaKeys();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        Map secondLoginParams = new HashMap<>(9);
//        secondLoginParams.put("mac", "4C-44-5B-FC-C0-B0");
        secondLoginParams.put("signature", signature);
        secondLoginParams.put("userName", userName);
        secondLoginParams.put("randomKey", randomKey);
        //Encode the public key to base64, in order to transfer over HTTP protocol.
        PUBLIC_KEY = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        PRIVATE_KEY = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        secondLoginParams.put("publicKey", PUBLIC_KEY);
        secondLoginParams.put("encrytType", "MD5");
        secondLoginParams.put("ipAddress", ADDRESS);
        secondLoginParams.put("clientType", "WINPC_V2");
        secondLoginParams.put("userType", "0");
        String secondResponseString = sendPostOrPut(AUTH_URL, secondLoginParams, POST);
        JSONObject secondResponse = JSONObject.parseObject(secondResponseString);
        System.out.println("----------------");
        System.out.println(secondResponse);

        TOKEN_VALUE = secondResponse.getString(TOKEN);
        SECRETKEY_WITH_SA =  secondResponse.getString("secretKey");
        DURATION = secondResponse.getString("duration");
        SECRET_VECTOR = secondResponse.getString("secretVector");
        USER_ID = secondResponse.getString("userId");
        System.out.println(String.format("token is : %s", TOKEN_VALUE));
        System.out.println(String.format("duration is : %s", DURATION));
        System.out.println(String.format("secretKeyWithRsa is : %s",SECRETKEY_WITH_SA));
        System.out.println(String.format("secretVectorWithRsa is : %s",SECRET_VECTOR ));
        System.out.println(String.format("your privateKeyWithBase64,you can decrypt secretKeyWithRsa and secretVectorWithRsa with it: %s", PRIVATE_KEY));
        //Step3: Keep alive and Update the token.
        KeepAlive keepAlive = new KeepAlive();
        keepAlive.start();
        //Login is over,do your own things with the token...

        MqConnection mqConnectionExample = new MqConnection();
//        Map d = new HashMap<>(2);
//        d.put("token", TOKEN_VALUE);
//        d.put("orgCode", "001");

//
//        String dives = sendPostOrPut(URL_DSS+"admin/API/tree/devices", d,
//                POST);
//        System.out.println("-----------fgsfdgsfdg-----");
//        System.out.println(dives);

    }
    static String sendPostOrPut(String url, Map params, String requestMode) throws IOException {
        if (!requestMode.equals(POST) && !requestMode.equals(PUT)) {
            return null;
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpEntityEnclosingRequestBase httpRequest = null;
        if (requestMode.equals(POST)) {
            httpRequest = new HttpPost(url);
        } else if (requestMode.equals(PUT)) {
            httpRequest = new HttpPut(url);
        }
        StringEntity entity = new StringEntity(JSON.toJSONString(params), "UTF8");

        httpRequest.setEntity(entity);
        httpRequest.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (params.containsKey(TOKEN)) {
            httpRequest.setHeader("X-Subject-Token", params.get(TOKEN).toString
                    ());
        }
        CloseableHttpResponse response = httpClient.execute(httpRequest);
        HttpEntity responseEntity = response.getEntity();
        //In order to avoid messy code,encode the response data in UTF-8.
        String reply = EntityUtils.toString(responseEntity, "UTF-8");
        //Release resources finally.
        if (httpClient != null) {
            httpClient.close();

        }
        if (response != null) {
            response.close();
        }
        return reply;
    }
    static String generateSignature(String userName, String password, String realm, String randomKey) {

        String temp1 = DigestUtils.md5Hex(password);
        String temp2 = DigestUtils.md5Hex(userName + temp1);
        String temp3 = DigestUtils.md5Hex(temp2);
        String temp4 = DigestUtils.md5Hex(userName + ":" + realm + ":" + temp3);
        //Retain the temp4,in order to calculate a signature for update token later.
                SIGNATURE_MD5_TEMP4 = temp4;
        String signature = DigestUtils.md5Hex(temp4 + ":" + randomKey);
        return signature;
    }
    /**
     * Generate RSA public key and private key.
     * @return KeyPair
     * @throws Exception
     */
    static KeyPair getRsaKeys() throws Exception {
        Provider provider = Security.getProvider("SunRsaSign");
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", provider);
        keyPairGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        return keyPair;
    }
}



