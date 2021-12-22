package com.yu.dubbo.core.cluster.loadbalance;

import com.alibaba.fastjson.JSONObject;
import com.yu.dubbo.core.protocol.RequestDomain;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConsistentHashLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "consistenthash";
    private final ConcurrentMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> providers, RequestDomain requestDomain) {
        String key = requestDomain.getClassName() + requestDomain.getMethodName() + JSONObject.toJSONString(requestDomain.getParamTypeNames());
        int identityHashCode = System.identityHashCode(providers.size());
        ConsistentHashSelector selector = selectors.get(key);
        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(key, new ConsistentHashSelector(providers, identityHashCode));
            selector = selectors.get(key);
        }
        return selector.select(key);
    }

    private static final class ConsistentHashSelector {

        private final TreeMap<Long, String> virtualInvokers;

        private final int replicaNumber;

        private final int identityHashCode;


        ConsistentHashSelector(List<String> providers, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;
            this.replicaNumber = 160;
            for (String provider : providers) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(provider + i);
                    for (int h = 0; h < 4; h++) {
                        long m = hash(digest, h);
                        virtualInvokers.put(m, provider);
                    }
                }
            }
        }

        public String select(String params) {
            byte[] digest = md5(params);
            return selectForKey(hash(digest, 0));
        }

        private String selectForKey(long hash) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hash, true).firstEntry();
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }

        private long hash(byte[] digest, int number) {
            return (((long) (digest[3 + number * 4] & 0xFF) << 24)
                    | ((long) (digest[2 + number * 4] & 0xFF) << 16)
                    | ((long) (digest[1 + number * 4] & 0xFF) << 8)
                    | (digest[number * 4] & 0xFF))
                    & 0xFFFFFFFFL;
        }

        private byte[] md5(String value) {
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.reset();
            byte[] bytes;
            try {
                bytes = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            md5.update(bytes);
            return md5.digest();
        }

    }
}
