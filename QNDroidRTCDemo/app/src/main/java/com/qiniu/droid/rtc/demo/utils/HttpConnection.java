package com.qiniu.droid.rtc.demo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Scanner;

public class HttpConnection {
    private static final int HTTP_TIMEOUT_MS = 3000;

    private final String mMethod;
    private final String mUrl;
    private final String mMessage;
    private String mContentType;

    public HttpConnection(String method, String url, String message) {
        mMethod = method;
        mUrl = url;
        mMessage = message;
    }

    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public String sendHttpMessage() {
        String ret = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(mUrl).openConnection();
            byte[] postData = new byte[0];
            if (mMessage != null) {
                postData = mMessage.getBytes("UTF-8");
            }
            connection.setRequestMethod(mMethod);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            boolean doOutput = false;
            if (mMethod.equals("POST")) {
                doOutput = true;
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(postData.length);
            }
            if (mContentType == null) {
                connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            } else {
                connection.setRequestProperty("Content-Type", mContentType);
            }

            // Send POST request.
            if (doOutput && postData.length > 0) {
                OutputStream outStream = connection.getOutputStream();
                outStream.write(postData);
                outStream.close();
            }

            // Get response.
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                InputStream responseStream = connection.getInputStream();
                String response = drainStream(responseStream);
                responseStream.close();
                ret = response;
            }
            connection.disconnect();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private String drainStream(InputStream in) {
        Scanner s = new Scanner(in).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
