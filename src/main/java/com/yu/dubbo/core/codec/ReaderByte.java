package com.yu.dubbo.core.codec;

import com.yu.dubbo.utils.CommonUtil;

import java.util.Arrays;

/**
 * @author Administrator
 * @title: ycz
 * @projectName bridge
 * @date 2021/10/29 0029下午 4:20
 */
public class ReaderByte {


    private byte[] bytes;

    private int index = 0;

    public ReaderByte(byte[] bytes) {
        this.bytes = bytes;
    }

    public Integer readInteger() {
        int value = 0;
        int limit = index + 4;
        int lastIndex = index + 3;
        for (; index < limit; index++) {
            int shift = (lastIndex - index) * 8;
            value += (bytes[index] & 0xFF) << shift;
        }
        return value;
    }

    public String readString() throws Exception {
        int len = readInteger();
        if (len == 0) {
            return null;
        }
        byte[] news = new byte[len];
        System.arraycopy(bytes, index, news, 0, len);

        index += len;

        return new String(news, "utf-8");
    }

    public byte[] readBytes() throws Exception {
        int len = readInteger();
        if (len == 0) {
            return null;
        }
        byte[] news = Arrays.copyOfRange(bytes, index, index += len);
        return news;
    }

    public Object readObject() throws Exception {
        String clsName = readString();
        if (clsName == null) {
            return null;
        }
        byte[] bufs = readBytes();

        Object r = CommonUtil.deserialize(bufs, Class.forName(clsName));
        return r;
    }

    public Object[] readObjectArray() throws Exception {
        int len = readInteger();
        if (len == 0) {
            return null;
        }

        Object[] result = new Object[len];
        for (int i = 0; i < len; i++) {
            result[i] = readObject();
        }

        return result;
    }

    public String[] readStringArray() throws Exception {
        int len = readInteger();
        if (len == 0) {
            return null;
        }
        String[] result = new String[len];
        for (int i = 0; i < len; i++) {
            result[i] = readString();
        }
        return result;
    }

    public byte readByte() {
        return bytes[index++];
    }

}
