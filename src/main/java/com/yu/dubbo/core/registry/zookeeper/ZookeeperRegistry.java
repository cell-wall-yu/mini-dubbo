package com.yu.dubbo.core.registry.zookeeper;

import com.alibaba.fastjson.JSONObject;
import com.yu.dubbo.core.SpringContextHolder;
import com.yu.dubbo.core.registry.RegistryFactory;
import com.yu.dubbo.core.registry.RegistryType;
import com.yu.dubbo.core.registry.domain.AppDeploy;
import com.yu.dubbo.core.registry.domain.AppServiceDomain;
import com.yu.dubbo.core.registry.domain.URL;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_ADDED;
import static org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type.NODE_REMOVED;

public class ZookeeperRegistry extends RegistryFactory {

    private static AtomicBoolean hasInit = new AtomicBoolean(false);
    private static final Lock LOCK = new ReentrantLock();
    private static List<Closeable> closeableList = new ArrayList<Closeable>();

    // Curator客户端
    private static CuratorFramework client = null;

    private static Executor executor = new Executor() {
        public void execute(Runnable command) {
            new Thread(command, "rainbow-zookeeper").start();
        }
    };

    private static Logger log = LoggerFactory.getLogger(ZookeeperRegistry.class);

    static {
        if (hasInit.compareAndSet(false, true)) {
            if (StringUtils.isEmpty(SpringContextHolder.getProperties("spring.zookeeper.address"))) {
                throw new RuntimeException("请检查zookeeper地址{spring.zookeeper.address}");
            }
            String address = SpringContextHolder.getProperties("spring.zookeeper.address");
            init(address);
        }
    }

    public static void init(String zkAddress) {
        if (StringUtils.isEmpty(zkAddress)) {
            throw new RuntimeException("zookeeper 地址不正确: " + zkAddress);
        }

        log.info("[bridge zookeeper] 初始化 zk: " + zkAddress);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                if (closeableList != null && closeableList.size() > 0) {
                    for (Closeable closeable : closeableList) {
                        try {
                            closeable.close();
                        } catch (Exception e) {
                        }
                    }
                }
                log.info("[rainbow zookeeper] 进程关闭，主动断开连接");
                if (client != null) {
                    client.close();
                    client = null;
                }
            }
        }));

        // 重连策略
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(Integer.MAX_VALUE, Integer.MAX_VALUE);

        // 实例化Curator客户端，Curator的编程风格可以让我们使用方法链的形式完成客户端的实例化
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder();
        client = builder
                // 使用工厂类来建造客户端的实例对象
                .threadFactory(new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "bridge-zookeeper");
                    }
                })
                .connectString(zkAddress)
                .sessionTimeoutMs(30000).retryPolicy(retryPolicy) // 设定会话时间以及重连策略
                .build();// 建立连接通道
        // 启动Curator客户端
        client.start();
    }


    /**
     * 读取子目录列表
     *
     * @param path
     * @return
     */
    public static List<String> readSubPath(String path) {
        try {
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {
                log.warn("[zookeeper] 节点不存在, path: {}, stat: {}", path, stat);
                return null;
            }
            List<String> childPaths = client.getChildren().forPath(path);

            List<String> result = new ArrayList<String>(childPaths.size());
            for (String string : childPaths) {
                result.add(URLDecoder.decode(string, "utf-8"));
            }
            return result;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * 读取子目录列表
     *
     * @param path
     * @return
     */
    public static byte[] readData(String path) {

        try {
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {
                log.warn("[zookeeper] 节点不存在, path: {}, stat: {}", path, stat);
                return null;
            }
            byte[] data = client.getData().forPath(path);

            return data;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }


    private static void checkAndCreatePath(String path) {
        try {
            Stat stat = client.checkExists().forPath(path);
            if (stat == null) {

                log.info("[zookeeper] 节点不存在：" + path);
                int index = path.lastIndexOf("/");

                if (index != 0) {

                    String parent = path.substring(0, index);
                    checkAndCreatePath(parent);
                }
                log.info("[zookeeper] 开始创建节点：" + path);
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
            }
            return;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public AppServiceDomain.Provider getProviderFromRegistry(String serviceName) {
        // provider
        String proivderFolder = RegistryType.AppService.getPath() + "/" + serviceName + "/provider";

        List<String> providers = readSubPath(proivderFolder);

        if (providers != null && providers.size() > 0) {
            return (new AppServiceDomain.Provider(serviceName, providers));
        }
        return null;
    }

    /**
     * 生产者注册
     *
     * @param appName
     * @param appDeploy
     */
    @Override
    public void registerAppDeploy(String appName, AppDeploy appDeploy) {

        try {
            String deployMsg = JSONObject.toJSONString(appDeploy);

            // 创建服务根目录
            String parentPath = RegistryType.AppDeploy.getPath() + "/" + appName;

            // 创建临时子节点
            String subPath = URLEncoder.encode(deployMsg, "utf-8");
            String path = parentPath + "/" + subPath;

            // 检查父节点是否存在
            checkAndCreatePath(parentPath);

            Stat stat = client.checkExists().forPath(path);

            // zk会有缓存，必须要删除节点后 才能新增
            if (stat != null) {
                client.delete().forPath(path);
            }

            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 获取部署信息
     *
     * @return
     */
    @Override
    public List<AppDeploy> getAppDeployInfo() {
        List<String> apps = readSubPath(RegistryType.AppDeploy.getPath());
        if (apps == null || apps.size() == 0) {
            return null;
        }
        Collections.sort(apps);
        List<AppDeploy> appList = new ArrayList<>();
        for (String appName : apps) {
            List<String> deplyMsgs = readSubPath(RegistryType.AppDeploy.getPath() + "/" + appName);
            for (String deployMsg : deplyMsgs) {
                AppDeploy appDeploy = JSONObject.parseObject(deployMsg, AppDeploy.class);
                appList.add(appDeploy);
            }
        }
        return appList;
    }

    @Override
    public void registerProvider(URL url) {
        LOCK.lock();
        try {
            // 创建服务根目录
            String parentPath = RegistryType.AppService.getPath() + "/" + url.getClassName() + "/provider";
            // 创建临时子节点
            String subPath = URLEncoder.encode(url.getAddress(), "utf-8");
            String path = parentPath + "/" + subPath;
            // 检查父节点是否存在
            checkAndCreatePath(parentPath);
            Stat stat = client.checkExists().forPath(path);
            // zk会有缓存，必须要删除节点后 才能新增
            if (stat != null) {
                client.delete().forPath(path);
            }

            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            LOCK.unlock();
        }

    }

    @Override
    public void registerConsumer(URL url) {

        LOCK.lock();
        try {
            // 创建服务根目录
            String parentPath = RegistryType.AppService.getPath() + "/" + url.getClassName() + "/customer";

            // 创建临时子节点
            String subPath = URLEncoder.encode(url.getAddress(), "utf-8");
            String path = parentPath + "/" + subPath;

            // 检查父节点是否存在
            checkAndCreatePath(parentPath);

            Stat stat = client.checkExists().forPath(path);

            // zk会有缓存，必须要删除节点后 才能新增
            if (stat != null) {
                client.delete().forPath(path);
            }

            client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path);

            // 监听服务
            String providerPath = RegistryType.AppService.getPath() + "/" + url.getClassName() + "/provider";

            final TreeCache treeCache = new TreeCache(client, providerPath);
            treeCache.getListenable().addListener(new TreeCacheListener() {
                public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
                    switch (event.getType()) {
                        case NODE_ADDED:
                            log.info("NODE_ADDED" + event.getData().getPath());
                            zkNotify(event.getData().getPath().substring((RegistryType.AppService.getPath() + "/").length(), event.getData().getPath().indexOf("/provider")), NODE_ADDED,
                                    null);
                            break;
                        case NODE_REMOVED:
                            log.info("NODE_REMOVED" + event.getData().getPath());
                            String provider = URLDecoder.decode(event.getData().getPath().substring((event.getData().getPath().indexOf("/provider")) + "/provider/".length(), event.getData().getPath().length()), "utf-8");
                            zkNotify(event.getData().getPath().substring((RegistryType.AppService.getPath() + "/").length(), event.getData().getPath().indexOf("/provider")), NODE_REMOVED,
                                    provider);
                            break;
                        default:
                            break;
                    }
                }
            });
            treeCache.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            LOCK.unlock();
        }
    }


    @Override
    public void destroy() {
        if (client != null) {
            client.close();
        }
    }
}
