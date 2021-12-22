package com.yu.dubbo.core.cluster.loadbalance;

import com.google.common.base.Splitter;
import com.yu.dubbo.core.cluster.LoadBalance;
import com.yu.dubbo.core.protocol.RequestDomain;

import java.util.List;
import java.util.Map;

public abstract class AbstractLoadBalance implements LoadBalance {

    @Override
    public String select(List<String> providers, RequestDomain requestDomain) {
        if (providers == null || providers.isEmpty())
            return null;
        if (providers.size() == 1)
            return providers.get(0);
        return doSelect(providers, requestDomain);
    }

    protected abstract String doSelect(List<String> providers, RequestDomain requestDomain);

    protected static int getWeight(String provider) {
        String params = provider.substring(provider.indexOf("?") + 1, provider.length());
        Map<String, String> split = Splitter.on("&").withKeyValueSeparator("=").split(params);
        String weight = split.get("weight");
        return Integer.valueOf(weight);
    }
}
