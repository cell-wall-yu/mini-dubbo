package com.yu.dubbo.core.codec;

import com.yu.dubbo.core.protocol.RequestDomain;
import com.yu.dubbo.core.protocol.ResponseDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:27
 */
public class CodecUtil {

    private static Logger log = LoggerFactory.getLogger(CodecUtil.class);

    /**
     * 请求参数编码
     *
     * @param domain
     * @return
     */
    public static byte[] encodeRequest(RequestDomain domain) throws IOException {
        try {

            WriterByte writer = new WriterByte();
            writer.writeString(domain.getRequestNo());
            writer.writeString(domain.getClassName());
            writer.writeString(domain.getMethodName());
            writer.writeStringArray(domain.getParamTypeNames());
            writer.writeObjectArray(domain.getParamInputs());
            return writer.toByte();

        } catch (Exception e) {
            log.error("[CodecUtil] requestDomain encode error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 请求参数解码
     */
    public static RequestDomain decodeRequest(byte[] bytes) throws IOException, ClassNotFoundException {
        try {
            ReaderByte reader = new ReaderByte(bytes);

            String requestNo = reader.readString();
            String className = reader.readString();
            String methodName = reader.readString();
            String[] paramTypeNames = reader.readStringArray();
            Object[] paramInputs = reader.readObjectArray();

            RequestDomain domain = new RequestDomain();
            domain.setRequestNo(requestNo);
            domain.setClassName(className);
            domain.setMethodName(methodName);
            domain.setParamTypeNames(paramTypeNames);
            domain.setParamInputs(paramInputs);

            return domain;

        } catch (Exception e) {
            log.error("[CodecUtil] requestDomain decode error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 返回值解码
     *
     * @param bytes
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static ResponseDomain decodeResponse(byte[] bytes) {
        try {
            ReaderByte reader = new ReaderByte(bytes);

            Integer code = reader.readInteger();
            Integer costTime = reader.readInteger();
            String message = reader.readString();
            Object result = reader.readObject();

            ResponseDomain domain = new ResponseDomain();
            domain.setCode(code);
            domain.setCostTime(costTime);
            domain.setMessage(message);
            domain.setResult(result);

            return domain;

        } catch (Exception e) {
            try {
                log.error(new String(bytes, "utf-8"));
                log.error(new String(bytes, "gbk"));
            } catch (Exception e2) {
            }
            log.error("[CodecUtil] responseDomain decode error：" + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 返回值解码
     *
     * @param domain
     * @return
     * @throws IOException
     */
    public static byte[] encodeResponse(ResponseDomain domain) throws IOException {
        try {

            WriterByte writer = new WriterByte();
            writer.writeInteger(domain.getCode());
            writer.writeInteger(domain.getCostTime());
            writer.writeString(domain.getMessage());
            writer.writeObject(domain.getResult());
            return writer.toByte();
        } catch (Exception e) {
            log.error("[CodecUtil] responseDomain encode error：" + e.getMessage(), e);
        }
        return null;
    }
}
