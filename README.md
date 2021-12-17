# mini-dubbo
## 说明

> 轻量级远程过程调用（RPC）框架,简单配置,使用简单
> 
> 使用zookeeper作为注册中心
> 
> 注册中心主要是服务提供者，消费者的地址（在类上面使用@Provider注解提供者就会注册上去,使用@Reference创建代理过程中就会将消费者注册上去）
>
>zookeeper中的path /mini-dubbo/app-service/接口的reference path(com.yu.user.api.UserApi）/provider/(接口服务地址)11.11.12.130:8080/{项目名称}/服务器监听的地址

![zookeeper](https://user-images.githubusercontent.com/57479461/142148532-e8c0cc00-778f-4e2a-92f3-0380e81a0f6a.png)

## 原理

> 主要是通过使用接口的代理对象，类似调用本地方法一样，使用代理对象中的方法传入参数，代理对象执行方法时首先去执行invoker方法,invoker方法可以获取到执行的接口类，方法，参数，通过包装成
> 一个请求对象然后编码，然后获取注册中的服务提供者的暴露的请求地址，发送一个post请求，而服务端收到请求后将请求对象进行解码,获取请求中的接口类，方法，参数，然后找到本地的接口类实现类去执行
> 获取返回值进行包装编码再返回给消费端，消费端然后解码获取返回结果，通过invoker返回出去。

### 知识点
>数据协议编解码
> 
> 服务端另外再开一个servlet提供服务，专门处理PRC请求
> 
>jdk动态代理和反射机制

```java
(T) Proxy.newProxyInstance(AppClient.class.getClassLoader(), new Class<?>[]{interfaces}, (proxy, method, args) -> {
            AppClientHandler handler = new AppClientHandler(interfaces, method, args);
            return executor.submit(handler).get();
        });
```

>服务支持集群部署,消费是通过轮询负载均衡

## 使用
1 首先你可将代码clone到本地然后install一下将jar保存到本地仓库中

2 引入maven
```xml
        <dependency>
            <groupId>com.yu</groupId>
            <artifactId>mini-dubbo</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
```
3 关键步骤

3.1 服务提供者配置文件
```yaml
spring:
  application:
    name: shop-user
  # 注册中心 默认就是zookeeper
  registry: zookeeper
  zookeeper:
    address: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```
3.2 服务消费者配置文件
```yaml
spring:
  application:
    name: shop-order
  # 注册中心 默认就是zookeeper
  registry: zookeeper
  zookeeper:
    address: 127.0.0.1:2181,127.0.0.1:2182,127.0.0.1:2183
```
3.3 服务提供者，自动配置自定义服务提供者servlet，无需配置
> 主要将服务提供者的地址注册到注册中心，提供非消费者监听

3.4 消费者调用服务提供方的接口
> 通过使用mini-dubbo中的@Reference注解创建接口提供者的代理对象（该对象是在server启动时创建）类似于使用@Autowired进行赋值的方式

```java
@RestController
public class OrderController {
    @Reference
    public static UserApi userApi;
    @Reference
    public TestApi testApi;
}
```

3.5 服务订阅，下线
> 无论是提供者还是消费者先启动注册中心的信息都会拉取到本地缓存,而服务的下线是zookeeper的机制通知消费者清除缓存

![mini-dubbo](https://user-images.githubusercontent.com/57479461/140035605-bfb9d16a-0de4-453b-9791-db820e9f903a.png)
