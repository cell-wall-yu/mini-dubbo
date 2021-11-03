# mini-dubbo
## 说明
轻量级远程过程调用（RPC）框架,简单配置,使用简单

使用zookeeper作为注册中心

继承httpservlet监听指定的uri

客户端向指定的uri地址发送请求

使用kryo进行对象序列化

服务端监听指定的uri地址将参数解码去服务端调用接口执行并返回编码结果

客户端接受返回值进行解码

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
    name: shop-user
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

![未命名文件](https://user-images.githubusercontent.com/57479461/139415390-64c78ecf-ec14-4baf-bb24-d641e5ea164d.png)
