package com.yu.dubbo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:16
 */
public class HttpPostUtil {

    static String BOUNDARY = java.util.UUID.randomUUID().toString();
    static String PREFIX = "--", LINEND = "\r\n";
    static String MULTIPART_FROM_DATA = "multipart/form-data";
    static String CHARSET = "UTF-8";
    private static final int ReadTimeout = 120000;
    private static final int ConnectionTimeout = 30000;

    private static Logger log = LoggerFactory.getLogger(HttpPostUtil.class);

    public static byte[] request(String url, Map<String, String> params, byte[] data) {
        return request(url, params, null, data);
    }

    public static byte[] request(String url, Map<String, String> params, Map<String, String> headers, byte[] data) {
        try {

            Map<String, byte[]> files = null;
            if (data != null) {
                files = new HashMap<>();
                files.put("upload", data);
            }

            URL uri = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) uri.openConnection();

            conn.setConnectTimeout(ConnectionTimeout);
            conn.setReadTimeout(ReadTimeout);

            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("connection", "keep-alive");
            conn.setRequestProperty("charset", CHARSET);
            conn.setRequestProperty("Content-Type", MULTIPART_FROM_DATA + ";boundary=" + BOUNDARY);

            // http 头
            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            // 首先组拼文本类型的参数
            StringBuilder sb = new StringBuilder();
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sb.append(PREFIX);
                    sb.append(BOUNDARY);
                    sb.append(LINEND);
                    sb.append("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + LINEND);
                    sb.append("Content-Type: text/plain" + LINEND);
                    sb.append(LINEND);
                    sb.append(entry.getValue());
                    sb.append(LINEND);
                }
            }

            DataOutputStream outStream = new DataOutputStream(conn.getOutputStream());
            outStream.write(sb.toString().getBytes());

            // 发送文件数据
            if (files != null) {
                for (Map.Entry<String, byte[]> file : files.entrySet()) {
                    StringBuilder sb1 = new StringBuilder();
                    sb1.append(PREFIX);
                    sb1.append(BOUNDARY);
                    sb1.append(LINEND);
                    sb1.append("Content-Disposition: form-data; name=\"j5eQkZqZlpOa\"; filename=\"" + file.getKey() + "\"" + LINEND);
                    sb1.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINEND);
                    sb1.append(LINEND);

                    outStream.write(sb1.toString().getBytes());
                    outStream.write(file.getValue());
                    outStream.write(LINEND.getBytes());
                }
            }
            // 请求结束标志
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINEND).getBytes();
            outStream.write(end_data);
            outStream.flush();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            InputStream is = conn.getInputStream();
            int len = 0;
            byte[] buf = new byte[4096];
            while ((len = is.read(buf)) != -1) {
                bos.write(buf, 0, len);
            }

            outStream.close();
            conn.disconnect();
            conn = null;
            byte[] returns = bos.toByteArray();
            bos.close();
            bos = null;
            return returns;
        } catch (Exception e) {
            log.error("远程调用出错, url: {}, message:{} ", url, e.getMessage(), e);
            throw new RuntimeException("网络通信异常");
        }
    }
}
