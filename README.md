# mini-dubbo
轻量级远程过程调用（RPC）框架,简单配置,使用简单

使用zookeeper作为注册中心

继承httpservlet监听指定的uri

客户端向指定的uri地址发送请求

使用kryo进行对象序列化

服务端监听指定的uri地址将参数解码去服务端调用接口执行并返回编码结果

客户端接受返回值进行解码


![未命名文件](https://user-images.githubusercontent.com/57479461/139415390-64c78ecf-ec14-4baf-bb24-d641e5ea164d.png)
