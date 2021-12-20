package com.yu.dubbo.core.cluster.loadbalance;

import com.google.common.base.Splitter;
import com.yu.dubbo.core.cluster.LoadBalance;

import java.util.List;
import java.util.Map;

public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public String select(List<String> providers, String interfaceName, String methodName) throws Exception {
        if (providers == null || providers.isEmpty())
            return null;
        if (providers.size() == 1)
            return providers.get(0);
        return doSelect(providers, interfaceName, methodName);
    }

    protected abstract String doSelect(List<String> providers, String interfaceName, String methodName);

    protected static int getWeight(String provider) {
        String params = provider.substring(provider.indexOf("?") + 1, provider.length());
        Map<String, String> split = Splitter.on("&").withKeyValueSeparator("=").split(params);
        String weight = split.get("weight");
        return Integer.valueOf(weight);
    }
}
