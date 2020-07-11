package top.hcode.client;



import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import top.hcode.commons.MsgModel;

import javax.swing.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/7 20:36
 * @Description: 重连检测狗，当发现当前的链路不稳定关闭之后（触发离线状态的方法），进行12次重连
 */

@ChannelHandler.Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements TimerTask, ChannelHandlerHolder {

    private final Bootstrap bootstrap;
    private final Timer timer;
    private final int port;

    private final String host;

    private volatile boolean reconnect = true;
    private int attempts; // 当前尝试重连次数

    private final static int MAX_RECONNECT_COUNT = 12; //重连次数

    public ConnectionWatchdog(Bootstrap bootstrap, Timer timer, int port, String host, boolean reconnect) {
        this.bootstrap = bootstrap;
        this.timer = timer;
        this.port = port;
        this.host = host;
        this.reconnect = reconnect;
    }

    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };

    public static String format(Date date) {
        return df.get().format(date);
    }

    /**
     * channel链路每次active的时候，将其连接的次数重新☞ 0
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        if (attempts>0) {
            ClientFrame clientFrame = ChatRoomClient.clientFrame;
            clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                    null, "[系统消息] " + format(new Date()),
                    "当前客户端重连成功！您已成功进入聊天室！", clientFrame.sysVertical, true);
        }
        attempts = 0;//重连成功，将重连次数重置为0，方便下次重连
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        /**
         *  判断是否开启重连操作，需要鉴定是否为网络异常条件，若是手动断链退出聊天室的，
         *  ChatRoomClient.isReConnect为false 不开启重连操作，若是网络异常断链，则进行12次重连操作
         */
        if (reconnect && ChatRoomClient.isReConnect) {
            ClientFrame clientFrame = ChatRoomClient.clientFrame;
            if (attempts==0){
                clientFrame.repeatNameCount.clear(); // 断连数据清空
                clientFrame.users_model.clear();
                clientFrame.users_map.clear();
                clientFrame.users_label.setText("聊天室内人数：0");
                //清空当前的私聊窗口
                Set<Map.Entry<String, ClientFrame.privateChatFrame>> entrySet = clientFrame.privateChatFrameMap.entrySet();
                for (Map.Entry<String, ClientFrame.privateChatFrame> entry : entrySet) {
                    entry.getValue().dispose(); //关闭对应窗口
                }

                JOptionPane.showMessageDialog(clientFrame.frame, "┭┮﹏┭┮对不起！由于您与服务器的连接处于离线状态，系统君开始为您尝试进行重连...",
                        "提示", JOptionPane.INFORMATION_MESSAGE);
            }
            if (attempts < MAX_RECONNECT_COUNT) { //重连失败，且小于最大重连次数，继续进行重连。
                attempts++;
                clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                        null, "[系统消息] " + format(new Date()),
                        "(ง •_•)ง系统君正在为您尝试第"+attempts+"次重连服务器，请稍后....", clientFrame.sysVertical, true);
                //重连的间隔时间会越来越长
                int timeout = 2 << attempts;
                timer.newTimeout(this, timeout, TimeUnit.MILLISECONDS);
            }else{ // 超过最大重连次数，重连失败，重置聊天室一切参数
                clientFrame.showEscDialog("<( _ _ )>对不起！由于系统君多次重连依旧失败，请您重启客户端再次尝试！", null);
            }
        }
        ctx.fireChannelInactive();
    }


    /**
     * @MethodName
     * @Params  * @param null
     * @Description 定时任务，处理重连操作
     * @Return
     * @Since 2020/7/11
     */
    @Override
    public void run(Timeout timeout) throws Exception {

        ChannelFuture future;
        synchronized (bootstrap) {
            bootstrap.handler(new ChannelInitializer<Channel>() {

                @Override
                protected void initChannel(Channel ch) throws Exception {

                    ch.pipeline().addLast(handlers());
                }
            });
            future = bootstrap.connect(host, port);
        }
        //future对象
        future.addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture f) throws Exception {
                boolean succeed = f.isSuccess();
                ClientFrame clientFrame = ChatRoomClient.clientFrame;

                //如果重连失败，则调用ChannelInactive方法，再次出发重连事件，一直尝试12次，如果失败则不再重连
                if (!succeed) {
                    clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                            null, "[系统消息] " + format(new Date()),
                            "X﹏X失败！此次重连服务器失败！......", clientFrame.sysVertical, true);

                    f.channel().pipeline().fireChannelInactive();
                } else { //重连成功，重新回到聊天室
                    clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                            null, "[系统消息] " + format(new Date()),
                            "(●'◡'●)成功！经过系统君不懈重连，您已重新进入聊天室！", clientFrame.sysVertical, true);
                    ChatRoomClient.socketChannel=(SocketChannel) f.channel(); //重点！ 记得重新赋值！
                }
            }
        });

    }

}