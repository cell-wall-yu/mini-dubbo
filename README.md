# mini-dubbo
## 说明
轻量级远程过程调用（RPC）框架,简单配置,使用简单

使用zookeeper作为注册中心
        注册地址主要是服务提供者的地址（在类上面使用@Provider注解就会注册上去）
                /mini-dubbo
                        /app_service
                                /接口的referance path(com.yu.user.api.UserApi）
                                        /11.11.12.130:8080/user/自定义服务器监听的地址

自定义数据协议进行编解码

自定义服务端继承httpServlet，处理客户端发送的RPC数据解码后处理本地调用结果编码返回

自定义客户端使用jdk代理和反射，获取调用的代理类，方法，参数
```java
(T) Proxy.newProxyInstance(AppClient.class.getClassLoader(), new Class<?>[]{interfaces}, (proxy, method, args) -> {
            AppClientHandler handler = new AppClientHandler(interfaces, method, args);
            return executor.submit(handler).get();
        });
```

服务提供者支持集群部署
##使用
1.首先你可将代码clone到本地然后install一下将jar保存到本地仓库中

2.引入maven
```xml
        <dependency>
            <groupId>com.yu</groupId>
            <artifactId>mini-dubbo</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
3.关键步骤

3.1服务提供者配置文件
```yaml
spring:
  application:
    name: shop-user
  # 注册中心 默认就是zookeeper
  registry: zookeeper
  zookeeper:
    address: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```
3.2服务消费者配置文件
```yaml
spring:
  application:
    name: shop-order
  # 注册中心 默认就是zookeeper
  registry: zookeeper
  zookeeper:
    address: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```
3.1服务提供者，需要在配置类中自定义服务提供者servlet
```java

    // 可以自定义uri地址
    @Bean
    public ServletRegistrationBean customServlet() {
        return RegisterUtil.customServlet(null);
    }
```
3.2消费者调用服务提供方的接口
```java
    @GetMapping
    public User getUser() {
        // 关键AppClient.proxy
        UserApi userApi = AppClient.proxy(UserApi.class);
        User dto = userApi.getUserById("1");
        return dto;
    }
```

![未命名文件 (1)](https://user-images.githubusercontent.com/57479461/140035605-bfb9d16a-0de4-453b-9791-db820e9f903a.png)
