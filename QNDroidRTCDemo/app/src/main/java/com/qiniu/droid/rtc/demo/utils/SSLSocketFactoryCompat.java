package com.qiniu.droid.rtc.demo.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SSLSocketFactoryCompat extends SSLSocketFactory {
    private static SSLSocketFactory mDelegate;

    public SSLSocketFactoryCompat() {
        mDelegate = HttpsURLConnection.getDefaultSSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return mDelegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mDelegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return toTLS(mDelegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return toTLS(mDelegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return toTLS(mDelegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return toTLS(mDelegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return toTLS(mDelegate.createSocket(address, port, localAddress, localPort));
    }

    private Socket toTLS(Socket socket) {
        if (socket instanceof SSLSocket) {
            String[] originalEnabledProtocols = ((SSLSocket) socket).getEnabledProtocols();
            List<String> filteredProtocols = new ArrayList<>();
            for (String s : originalEnabledProtocols) {
                if (!s.equals("SSLv3")) {
                    filteredProtocols.add(s);
                }
            }
            ((SSLSocket) socket).setEnabledProtocols(filteredProtocols.toArray(new String[filteredProtocols.size()]));
        }
        return socket;
    }
}
