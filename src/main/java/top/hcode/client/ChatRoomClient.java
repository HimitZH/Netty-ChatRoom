package top.hcode.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.internal.StringUtil;
import top.hcode.commons.MsgModel;
import top.hcode.commons.MsgPackDecoder;
import top.hcode.commons.MsgPackEncoder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/7 01:00
 * @Description:
 */
public class ChatRoomClient implements ActionListener {
    private String hostName;

    private int port;

    protected static ClientFrame clientFrame; //操作界面

    protected static Login login; //登录界面

    protected final HashedWheelTimer timer = new HashedWheelTimer();

    private int reConnectCount = 0;  //启动重连次数

    private final static int MAX_CONNECT_COUNT=5; //最大重连次数

    protected static ChatRoomClient chatRoomClient;

    private Bootstrap bootstrap;

    private EventLoopGroup group;

    public static SocketChannel socketChannel;

    public static String name;

    public static String order;

    public static String role;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public volatile static boolean isReConnect = true;

    private static final ThreadLocal<DateFormat> df = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        }
    };
    public static Date parse(String dateStr) throws ParseException {
        return df.get().parse(dateStr);
    }

    public static String format(Date date) {
        return df.get().format(date);
    }

    public void init(){
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true);
        final ConnectionWatchdog watchdog = new ConnectionWatchdog(bootstrap, timer, port, hostName, true) {

            public ChannelHandler[] handlers() {
                return new ChannelHandler[]{
                        this,
                        new IdleStateHandler(20, 10, 0, TimeUnit.SECONDS),
                        new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2, false),
                        new MsgPackDecoder(),
                        new LengthFieldPrepender(2),
                        new MsgPackEncoder(),
                        new ChatRoomClientHandler(clientFrame)
                };
            }
        };

        doConnect(watchdog);
    }


    public void start(int port) {
        this.port = port;
        this.hostName = "127.0.0.1";
        init();
    }

    public void start(String hostName, int port) {
        this.port = port;
        this.hostName = hostName;
        init();
    }


    protected void doConnect(ConnectionWatchdog watchdog) {
        ChannelFuture future;
        // 开始连接
        try {
            synchronized (bootstrap) {
                bootstrap.handler(new ChannelInitializer<Channel>() {  //初始化处理器

                    //初始化channel
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });

                future = bootstrap.connect(hostName, port);
            }
            future.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {

                    if (channelFuture.isSuccess()) { //成功连接
                        socketChannel = (SocketChannel) channelFuture.channel();
                        if (reConnectCount > 0){
                            clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                                    null, "[系统消息] " + format(new Date()), "(●ˇ∀ˇ●)经过系统君为您进行的"+reConnectCount+
                                            "次重连，您已成功进入聊天室！", clientFrame.sysVertical, true);

                            JOptionPane.showMessageDialog(clientFrame.frame, "(●'◡'●)重连成功！您已成功进入聊天室！",
                                    "提示", JOptionPane.INFORMATION_MESSAGE);
                            reConnectCount = 0;
                        }
                    } else { //未成功连接，进行重连
                        long reconnectTime = 2<<reConnectCount;
                        clientFrame.insertMessage(clientFrame.sysTextScrollPane, clientFrame.sysMsgArea,
                                null, "[系统消息] " + format(new Date()),
                                "连接失败！(ง •_•)ง系统君正在为您尝试进行第"+(++reConnectCount)+"次重连....", clientFrame.sysVertical, true);

                        channelFuture.channel().eventLoop().schedule(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                if(reConnectCount>=MAX_CONNECT_COUNT){ //重连5次失败，放弃重连，此时已经说明服务器宕机了。
                                    clientFrame.showEscDialog("┭┮﹏┭┮对不起，多次重连失败说明服务器已宕机，请联系管理人员！", null);
                                    reConnectCount = 0;
                                }else {
                                    doConnect(watchdog);
                                }
                            }
                        },reconnectTime , TimeUnit.SECONDS);
                    }
                }
            });
            future.sync();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    public void disconnect(){
        group.shutdownGracefully(); //优雅的关闭
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if (!cmd.equals("进入")) {
            name = clientFrame.name_textfield.getText();
        }
        switch (cmd) {
            case "进入": //进入初始化界面
                if(login.comboBox.getSelectedIndex() == 1){
                    String code = login.textField.getText();
                    if(StringUtil.isNullOrEmpty(code)){
                        JOptionPane.showMessageDialog(null, "权限密令不能为空！", "提示", JOptionPane.WARNING_MESSAGE);
                        break;
                    }else{
                        if (code.equals("HCODE")){
                            order = code;
                            clientFrame = new Admin(chatRoomClient);
                        }else {
                            JOptionPane.showMessageDialog(null, "权限密令错误，请重新输入！", "错误", JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                }else{
                    order = "user";
                    clientFrame = new User(chatRoomClient);
                }
                login.dispose();
                ClientFrameContext clientFrameContext = new ClientFrameContext(clientFrame);
                clientFrameContext.init();
                break;
            case "连接":
                //获取文本框里面的ip和port
                String strhost =clientFrame.host_textfield.getText();
                String strport =clientFrame.port_textfield.getText();
                if (!User.ipCheckHost(strhost)) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请检查ip格式是否准确！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (!User.ipCheckPort(strport)) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请检查端口号是否为0~65535之间的整数！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                //连接服务器;
                isReConnect = true; //每次连接都重置断线重连为true
                chatRoomClient.start(strhost, Integer.parseInt(strport));
                clientFrame.head_connect.setText("已连接");
                clientFrame.head_exit.setText("退出");
                clientFrame.port_textfield.setEditable(false);
                clientFrame.name_textfield.setEditable(false);
                clientFrame.host_textfield.setEditable(false);
                break;
            case "退出":
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "对不起，您现在不在聊天室，无法退出！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                isReConnect = false;
                clientFrame.showEscDialog("您已成功退出聊天室！",format(new Date()));
                break;
            case "发送":
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                String text = clientFrame.text_field.getText();
                if (!StringUtil.isNullOrEmpty(text)) {
                    String formatMsg = "<id>"+clientFrame.userId+"</id>"+"<msg>"+text+"</msg>";
                    MsgModel chatMsg = new MsgModel((byte) 2, (byte) 1, formatMsg);
                    chatRoomClient.getSocketChannel().writeAndFlush(chatMsg); //写入通道
                    clientFrame.text_field.setText("");
                }
                break;
            case "私聊":
                //服务器未连接
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }

                String selected = clientFrame.userlist.getSelectedValue();
                if (StringUtil.isNullOrEmpty(selected)){
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先选择所要操作的用户！！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                // 被屏蔽者无法进行私聊
                if (selected.endsWith("(已屏蔽)")) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "被屏蔽的用户无法进行私聊！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                //私聊自己
                if (selected.equals(clientFrame.getUserName(clientFrame.userId))) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "你不能私聊自己！", "警告", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                //已有私聊窗口
                if (clientFrame.privateChatFrameMap.containsKey(clientFrame.users_map.get(selected))) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "与该用户私聊窗口已存在，请不要重复私聊！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (clientFrame.users_map.containsKey(selected)) {
                    String selectedId = clientFrame.users_map.get(selected);
                    if (!clientFrame.privateChatFrameMap.containsKey(selectedId)) {
                        User.privateChatFrame chatFrame =clientFrame.new privateChatFrame("与" +selected+ "的私聊窗口", selected, selectedId);
                        clientFrame.privateChatFrameMap.put(selectedId, chatFrame);
                    }
                    String msg = "<id>" + clientFrame.users_map.get(selected) + "</id><order>1</order><msg>start</msg>";
                    MsgModel msgModel = new MsgModel((byte) 2, (byte) 2, msg);
                    chatRoomClient.getSocketChannel().writeAndFlush(msgModel); //写入通道
                    //建立沟通弹窗
                }
                break;
            case "屏蔽对方":
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                String selectedShield = clientFrame.userlist.getSelectedValue();
                if (StringUtil.isNullOrEmpty(selectedShield)){
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先选择所要操作的用户！！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }

                //如果是自己
                if (selectedShield.equals(clientFrame.getUserName(clientFrame.userId))) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "你不能屏蔽自己！", "警告", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                //不准屏蔽管理员！
                if (selectedShield.startsWith("[管理员]")) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "对不起，不支持屏蔽管理员！", "警告", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                //不能重复屏蔽！
                if (selectedShield.endsWith("(已屏蔽)")) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "对方已被屏蔽了！请不要重复操作！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (clientFrame.users_map.containsKey(selectedShield)) { //发送需要屏蔽用户的id
                    String msg = "<me>"+clientFrame.userId+"</me><other>"+clientFrame.users_map.get(selectedShield)+"</other>";
                    MsgModel msgModel = new MsgModel((byte) 2, (byte) 3, msg);
                    chatRoomClient.getSocketChannel().writeAndFlush(msgModel); //写入通道
                    clientFrame.users_map.put(selectedShield + "(已屏蔽)", clientFrame.users_map.get(selectedShield));
                    clientFrame.users_map.remove(selectedShield);
                }
                int index1 = clientFrame.users_model.indexOf(selectedShield);
                clientFrame.users_model.set(index1, selectedShield + "(已屏蔽)");
                break;
            case "取消屏蔽":
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                String unShield = clientFrame.userlist.getSelectedValue();
                if (StringUtil.isNullOrEmpty(unShield)){
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先选择所要操作的用户！！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (!unShield.endsWith("(已屏蔽)")) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "对方并未被屏蔽！", "警告", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                String unShieldName = unShield.substring(0, unShield.indexOf("(已屏蔽)"));
                String msg = "<me>"+clientFrame.userId+"</me><other>"+clientFrame.users_map.get(unShield)+"</other>";
                MsgModel msgModel = new MsgModel((byte) 2, (byte) 4, msg);
                chatRoomClient.getSocketChannel().writeAndFlush(msgModel); //写入通道
                int index2 = clientFrame.users_model.indexOf(unShield);
                clientFrame.users_model.set(index2, unShieldName);
                clientFrame.users_map.put(unShieldName, clientFrame.users_map.get(unShield));
                clientFrame.users_map.remove(unShield);
                break;
            case "清空系统消息":
                clientFrame.sysMsgArea.setText("");
                break;
            case "清空聊天消息":
                clientFrame.userMsgArea.setText("");
                break;
                // 管理员特殊按钮监听 需验证管理员身份。
            case "踢出":
                //服务器未连接
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }

                String toKickName = clientFrame.userlist.getSelectedValue();
                if (StringUtil.isNullOrEmpty(toKickName)){
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先选择所要操作的用户！！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                // 不能踢出自己
                if (toKickName.equals(clientFrame.getUserName(clientFrame.userId))) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "你不能踢出自己！", "警告", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                //不准踢出管理员！
                if (toKickName.startsWith("[管理员]")) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "对不起，不支持踢出管理员！", "错误", JOptionPane.ERROR_MESSAGE);
                    break;
                }
                MsgModel kickMsg = new MsgModel((byte) 1, (byte) 5, clientFrame.users_map.get(toKickName));
                chatRoomClient.getSocketChannel().writeAndFlush(kickMsg); //写入通道
                break;
            case "发送系统消息":
                if (ChatRoomClient.socketChannel==null||!ChatRoomClient.socketChannel.isActive()) {
                    JOptionPane.showMessageDialog(clientFrame.frame, "请先连接服务器进入聊天室！", "提示", JOptionPane.WARNING_MESSAGE);
                    break;
                }
                String adminSysMsg = clientFrame.text_field.getText();
                if (!StringUtil.isNullOrEmpty(adminSysMsg)) {
                    MsgModel adminSysModel = new MsgModel((byte) 2, (byte) 1, adminSysMsg);
                    chatRoomClient.getSocketChannel().writeAndFlush(adminSysModel); //写入通道
                    clientFrame.sysText_field.setText("");
                }
                break;
            default:
                System.out.println("监听出错，无效的按钮事件！");
                break;
        }

    }

}