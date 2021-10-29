package com.yu.dubbo.core.codec;

import com.yu.dubbo.utils.CommonUtil;

import java.io.ByteArrayOutputStream;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:22
 */
public class WriterByte {
    private ByteArrayOutputStream bos = new ByteArrayOutputStream();

    private static byte[] ZERO = WriterByte.intToByteArray(0);

    public void writeInteger(Integer value) throws Exception {
        byte[] buf = WriterByte.intToByteArray(value);
        bos.write(buf);
    }

    public void writeString(String value) throws Exception {
        if (value == null || value.matches("\\s*")) {
            bos.write(ZERO);
            return;
        }
        byte[] bufs = value.getBytes("utf-8");
        this.writeInteger(bufs.length);
        bos.write(bufs);
    }

    public void writeBytes(byte[] bufs) throws Exception {
        if (bufs == null || bufs.length == 0) {
            bos.write(ZERO);
            return;
        }
        this.writeInteger(bufs.length);
        bos.write(bufs);
    }

    public void writeByte(Byte b) throws Exception {
        bos.write(new byte[]{b});
    }

    public void writeObject(Object obj) throws Exception {
        if (obj == null) {
            bos.write(ZERO);
            return;
        }
        String type = obj.getClass().getName();
        this.writeString(type);
        byte[] buf = CommonUtil.serialize(obj);
        this.writeBytes(buf);
    }

    public void writeObjectArray(Object[] objs) throws Exception {
        if (objs == null || objs.length == 0) {
            bos.write(ZERO);
            return;
        }
        this.writeInteger(objs.length);
        for (Object object : objs) {
            this.writeObject(object);
        }
    }

    public void writeStringArray(String[] strings) throws Exception {
        if (strings == null || strings.length == 0) {
            bos.write(ZERO);
            return;
        }
        this.writeInteger(strings.length);
        for (String str : strings) {
            this.writeString(str);
        }
    }

    public byte[] toByte() {
        try {
            bos.flush();
            bos.close();
        } catch (Exception e) {
        }
        return bos.toByteArray();
    }

    /**
     * int到byte[] 由高位到低位
     *
     * @param i 需要转换为byte数组的整行值。
     * @return byte数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }
}


