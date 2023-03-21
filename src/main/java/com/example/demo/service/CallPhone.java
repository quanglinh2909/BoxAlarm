package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.CallCreator;
import com.twilio.type.PhoneNumber;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.example.demo.constants.Global.URL_CLOUD;

public class CallPhone {
//    twilio.account_sid=AC6e601872b3f78250d49ccde574a68baa
//    twilio.auth_token:764081af256a789ece2698be65828b0f
//    twilio.trial_number:+17163002384
//    twilio.url:+17163002384

    final static String ACCOUNT_SID = "AC6e601872b3f78250d49ccde574a68baa";
    final static String AUTH_TOKEN = "831a6e3991e70447c3eace090dca559c";
    final static String TRIAL_NUMBER = "+17163002384";
    final static String URL = "https://coffee-koala-5001.twil.io/assets/cuu.mp3";
    public CallPhone() {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

    }

    public void call(String phoneNumber) throws URISyntaxException {
        //utf-8
            PhoneNumber to = new PhoneNumber(phoneNumber);
            PhoneNumber from = new PhoneNumber(TRIAL_NUMBER);
            CallCreator callCreator = Call.creator(to, from, new URI(URL));
            Call call = callCreator.create();

    }

    public void callListPhone(String id) throws Exception {
        String t = CallPhone.sendGet(URL_CLOUD+"phonebook/get-all-by-alarm/", id);
        System.out.println("-------------------------callListPhone-------------------------");
        System.out.println(t);
        JSONArray jsonArray = JSON.parseArray(t);
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String phone = jsonObject.getString("Phone");
            //0355535733 -> +84355535733
            phone = "+84" + phone.substring(1);
            System.out.println(phone);
            call(phone);
        }
    }
    static String sendGet(String url, String id) throws IOException {

        HttpGet httpRequest = new HttpGet(url + id);
        CloseableHttpClient httpClient = HttpClients.createDefault();

        httpRequest.setHeader("Content-Type", "application/json;charset=UTF-8");

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



}
