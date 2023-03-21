package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.model.CustomerMqFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.*;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.example.demo.service.Login.URL_DSS;
import static com.example.demo.service.Login.USER_ID;
public class MqConnectionExample {
    static final String MQ_URL_PRFIX = "ssl://%s";
    static final String MQ_CONFIG_URL =URL_DSS+ "BRM/Config/GetMqConfig?token=%s";
    static final String TOPIC="topic";
    static final String QUEUE="queue";
    CallPhone callPhone = new CallPhone();

    public MqConnectionExample() throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(Login.PRIVATE_KEY);
        //Decrypt "SecretKey" and "SecretVector".
        String secretKey = decryptRSAByPrivateKey(Login.SECRETKEY_WITH_SA,privateKeyBytes);
        String secretVector = decryptRSAByPrivateKey(Login.SECRET_VECTOR, privateKeyBytes);
        //Step2: Call the "Get the MQ configuration" interface,in order to obtain the MQ password.
        //You can obtain the value of token from "LoginExample".
        String mqConfigResponseString = sendPost(String.format(MQ_CONFIG_URL,Login.TOKEN_VALUE), null);
        JSONObject mqConfigResponse = JSONObject.parseObject(mqConfigResponseString).getJSONObject("data");
        //Now,this password is encrypted by AES.Decrypt it with the "SecretKey"and "SecretVector" we have got in Step1.
        String userName = mqConfigResponse.getString("userName");

        String userPasswordWithAes = mqConfigResponse.getString("password");
        String mqUrlAddr = mqConfigResponse.getString("addr");
        //Decrypt the "Password".
        String userPassword = decryptWithAES7(userPasswordWithAes, secretKey, secretVector);
        //Step3: Initialize the MQ connection information,and start the messagelistening.
                //Replace it with a queueName or a topicName which you would listened.
                String messageName = "mq.alarm.msg.topic."+USER_ID;
        listenMqMessage(String.format(MQ_URL_PRFIX,mqUrlAddr),userName, userPassword, messageName,TOPIC);
        //The connection is over,do your own things here.
    }

     void listenMqMessage(String mqUrl,String userName,String passWord,String messageName,String messageType) throws Exception {
        if (!messageType.equals(TOPIC) && !messageType.equals(QUEUE)) {
            return ;
        }
        CustomerMqFactory factory = new CustomerMqFactory();
        factory.setUserName(userName);
        factory.setTrustStore("");
        factory.setPassword(passWord);
        factory.setBrokerURL(mqUrl);
        Connection connection = factory.createConnection();
        connection.start();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer;
        if(messageType.equals(TOPIC)){
            consumer = session.createConsumer(new ActiveMQTopic(messageName));
        }else{
            consumer = session.createConsumer(new ActiveMQQueue(messageName));
        }
        System.out.println("Start listening...");
        consumer.setMessageListener((message) -> {
            TextMessage textMessage = (TextMessage) message;
            try {
                String text = textMessage.getText();
                //Print the listening message.
                System.out.println("-------------------------------------------------------");
                JSONObject jsonObject = JSON.parseObject(text);
                String method = jsonObject.getString("method");
                String info = jsonObject.getString("info");
                if(method != null && method.equals("brms.notifyAlarms")){
                    JSONArray jsonArray = JSON.parseArray(info);
                    for(int i = 0; i < jsonArray.size() ; i++){
                        JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                        String deviceCode = jsonObject1.getString("deviceCode");
                        if (deviceCode != null ){
                            this.callPhone.callListPhone(deviceCode);
                        }
                    }
                }
                System.out.println(text);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static String sendPost(String url, Map params) throws IOException {
        if (params == null) {
            params = new HashMap<>(0);
        }
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(JSON.toJSONString(params), "UTF8");
        httpPost.setEntity(entity);
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        CloseableHttpResponse response = httpClient.execute(httpPost);
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

    static String decryptRSAByPrivateKey(String text, byte[] privateKeyEncoded)
            throws Exception {
        byte[] data = Base64.getDecoder().decode(text);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        Key privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] result = null;
        for (int i = 0; i < data.length; i += 256) {
            int to = (i + 256) < data.length ? (i + 256) : data.length;
            byte[] temp = cipher.doFinal(Arrays.copyOfRange(data, i, to));
            result = sumBytes(result, temp);
        }
        return new String(result, "UTF-8");
    }
    static byte[] sumBytes(byte[] bytes1, byte[] bytes2) {
        byte[] result = null;
        int len1 = 0;
        int len2 = 0;
        if (null != bytes1) {
            len1 = bytes1.length;
        }
        if (null != bytes2) {
            len2 = bytes2.length;
        }
        if (len1 + len2 > 0) {
            result = new byte[len1 + len2];
        }
        if (len1 > 0) {
            System.arraycopy(bytes1, 0, result, 0, len1);
        }
        if (len2 > 0) {
            System.arraycopy(bytes2, 0, result, len1, len2);
        }
        return result;
    }
    static String decryptWithAES7(String text, String aesKey, String aesVector)
            throws Exception {
        SecretKey keySpec = new SecretKeySpec(aesKey.getBytes("UTF-8"), "AES");
        //If your program run with an exception :"Cannot find any provider supporting AES/CBC/PKCS7Padding",you can replace "PKCS7Padding" with "PKCS5Padding".
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(aesVector.getBytes("UTF-8"));

        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
        byte[] encrypted = parseHexStr2Byte(text);
        byte[] originalPassByte = cipher.doFinal(encrypted);
        return new String(originalPassByte, "UTF-8");
    }
    static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1) {
            return null;
        }
        byte[] result = new byte[hexStr.length() / 2];
        for (int i = 0; i < hexStr.length() / 2; i++) {
            int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
            int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

}
