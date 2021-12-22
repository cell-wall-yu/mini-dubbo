package com.yu.dubbo.core.cluster.loadbalance;

import com.alibaba.fastjson.JSONObject;
import com.yu.dubbo.core.protocol.RequestDomain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class WeightRoundRobinLoadBalance extends AbstractLoadBalance {
    public static final String NAME = "weightroundrobin";

    protected static class WeightedRoundRobin {
        private AtomicLong current = new AtomicLong(0);

        private long getAndIncrement() {
            return current.getAndIncrement();
        }
    }

    private ConcurrentMap<String, WeightedRoundRobin> methodWeightMap = new ConcurrentHashMap<>();

    @Override
    protected String doSelect(List<String> providers, RequestDomain requestDomain) {
        String key = requestDomain.getClassName() + requestDomain.getMethodName() + JSONObject.toJSONString(requestDomain.getParamTypeNames());
        int length = providers.size(); // Number of invokers
        int maxWeight = 0; // The maximum weight
        int minWeight = Integer.MAX_VALUE; // The minimum weight
        final LinkedHashMap<String, IntegerWrapper> invokerToWeightMap = new LinkedHashMap<>();
        int weightSum = 0;
        //初始化maxWeight，minWeight，weightSum，invokerToWeightMap
        for (int i = 0; i < length; i++) {
            int weight = getWeight(providers.get(i));
            maxWeight = Math.max(maxWeight, weight); // Choose the maximum weight
            minWeight = Math.min(minWeight, weight); // Choose the minimum weight
            if (weight > 0) {
                invokerToWeightMap.put(providers.get(i), new IntegerWrapper(weight));
                weightSum += weight;
            }
        }
        // 获取自增调用次数
        WeightedRoundRobin weightedRoundRobin = methodWeightMap.get(key);
        if (weightedRoundRobin == null) {
            methodWeightMap.putIfAbsent(key, new WeightedRoundRobin());
            weightedRoundRobin = methodWeightMap.get(key);
        }
        // ?个人理解为当前调用总次数
        long currentSequence = weightedRoundRobin.getAndIncrement();
        //当权重不一样的时候，通过加权轮询获取到invoker,权值越大，则被选中的几率也越大
        if (maxWeight > 0 && minWeight < maxWeight) {
            long mod = currentSequence % weightSum;
            for (int i = 0; i < maxWeight; i++) {
                //遍历invoker的数量
                for (Map.Entry<String, IntegerWrapper> each : invokerToWeightMap.entrySet()) {
                    final String k = each.getKey();
                    //invoker的权重
                    final IntegerWrapper v = each.getValue();
                    if (mod == 0 && v.getValue() > 0) {
                        return k;
                    }
                    if (v.getValue() > 0) {
                        //当前invoker的可调用次数减1
                        v.decrement();
                        mod--;
                    }
                }
            }
        }
        // Round robin 权重一样的情况下，就取余的方式获取到invoker
        return providers.get((int) (currentSequence % length));
    }

    private static final class IntegerWrapper {
        private int value;

        public IntegerWrapper(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void decrement() {
            this.value--;
        }
    }
}
