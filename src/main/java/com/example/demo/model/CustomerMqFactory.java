package com.example.demo.model;

import org.apache.activemq.ActiveMQSslConnectionFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.cert.X509Certificate;


public class CustomerMqFactory extends ActiveMQSslConnectionFactory {


    @Override
    protected TrustManager[] createTrustManager() {
        return new TrustManager[]{
                new X509ExtendedTrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) {
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) {
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                    }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }

}


