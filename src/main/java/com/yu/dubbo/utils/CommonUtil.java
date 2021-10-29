package com.yu.dubbo.utils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * @author Administrator
 * @title: ycz
 * @projectName mini-dubbo
 * @date 2021/10/29 0029下午 4:08
 */
public class CommonUtil {

    private static Logger log = LoggerFactory.getLogger(CommonUtil.class);
    private static KryoPool kryoPool;

    private static final String yyyyMMdd = "yyyy-MM-dd";
    private static final String yyyyMMhhmmss = "yyyy-MM-dd HH:mm:ss";

    static {
        KryoPool.Builder builder = new KryoPool.Builder(new KryoFactory() {
            public Kryo create() {
                final Kryo kryo = new Kryo();
                kryo.register(Arrays.asList().getClass(), new ArraysAsListSerializer());
                kryo.setRegistrationRequired(false);
                kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
                kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
                return kryo;
            }
        });
        builder.softReferences();
        kryoPool = builder.build();
    }

    /**
     * 序列化对象
     *
     * @param obj
     * @return
     */
    public static byte[] serialize(Object obj) {

        Kryo kryo = null;
        try {
            kryo = kryoPool.borrow();
            Output output = new Output(10000, -1);

            kryo.writeObject(output, obj);
            output.close();

            return output.toBytes();
        } finally {
            kryoPool.release(kryo);
        }
    }

    /**
     * 反序列化对象
     *
     * @param by
     * @param type
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] by, Class<T> type) {
        if (null == by) {
            return null;
        }
        Kryo kryo = null;
        try {

            kryo = kryoPool.borrow();

            Input input = new Input(by);

            T outObject = kryo.readObject(input, type);
            input.close();
            return outObject;
        } finally {
            kryoPool.release(kryo);
        }
    }

    /**
     * 判断集合是否相等
     *
     * @param list1
     * @param list2
     * @return
     */
    public static boolean equals(List<String> list1, List<String> list2) {
        if (list1 == null || list2 == null) {
            return false;
        }

        // 长度都为0
        if (list1.size() == 0 && list2.size() == 0) {
            return true;
        }

        // 长度不相等 返回 false
        if (list1.size() != list2.size()) {
            return false;
        }

        for (String str1 : list1) {
            if (!list2.contains(str1)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 格式化时间
     *
     * @param date
     * @param format
     * @return
     */
    public static String format(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }


    public static String getAppHomePath() {
        try {
            String profile = SpringContextHolder.getProperties("spring.profiles.active");
            String active = "application-" + profile + ".yml";
            log.info("[spring-config] springProfile: " + active);
            Enumeration<URL> en = CommonUtil.class.getClassLoader().getResources(active);
            if (en.hasMoreElements()) {
                String path = en.nextElement().getPath();
                if (path != null) {

                    // windows
                    if ("\\".equals(File.separator)) {
                        path = path.replace("\\", "/");
                    }

                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }

                    path = path.substring(0, path.lastIndexOf("/"));

                    if (path.endsWith("conf")) {
                        path = path.substring(0, path.lastIndexOf("/"));
                    }
                    log.info("[spring-config] app home-path: " + path);
                    return path;
                }
            }
            return null;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public static String currentHttpAddress = null;

    /**
     * 获取机器ip+port
     *
     * @return
     */
    public static String getLocalServerAddress() {

        if (currentHttpAddress != null) {
            return currentHttpAddress;
        }

        String httpPort = null;

        if (httpPort == null) {
            httpPort = SpringContextHolder.getProperties("server.port");
        }

        if (httpPort == null) {
            throw new RuntimeException("无法获取当前服务监听端口号: server.port");
        }
        try {
            InetAddress addr = InetAddress.getLocalHost();
            // 获取本机ip
            String ip = addr.getHostAddress();
            currentHttpAddress = "http://" + ip + ":" + httpPort;
            return currentHttpAddress;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("无法获取当前机器IP地址");
        }

    }

    public static void main(String[] args) {
        System.out.println(format(new Date(), CommonUtil.yyyyMMdd));
        System.out.println(format(new Date(), CommonUtil.yyyyMMhhmmss));
    }
}
