package top.hcode.server;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.StringUtil;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/6 22:50
 * @Description:
 */
public class ChatRoomServer {


    public void start(String hostName,int port){
        start0(hostName,port);
    }
    public void start(int port){
        start0("127.0.0.1", port);
    }

    private void start0(String hostName,int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);  //处理请求连接的线程组
        EventLoopGroup workGroup = new NioEventLoopGroup();           // 处理读写业务的线程组 默认为CPU核数的两倍

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup) //将两个线程组加到服务器
                    .channel(NioServerSocketChannel.class) //指定通道类型
                    .option(ChannelOption.SO_BACKLOG, 256) // 连接等待队列大小
                    .childOption(ChannelOption.SO_KEEPALIVE, true) // 启用心跳保活机制
                    .childHandler(new ChatRoomServerInitializer());

            ChannelFuture future = serverBootstrap.bind(port).sync();// 开启服务

            future.channel().closeFuture().sync(); //监听关闭

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 非常优雅的关闭资源
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }
}