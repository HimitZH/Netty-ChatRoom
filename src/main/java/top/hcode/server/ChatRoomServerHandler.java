package top.hcode.server;


import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import top.hcode.commons.MsgModel;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


//处理业务的handler
public class ChatRoomServerHandler extends ChannelInboundHandlerAdapter {

    //验证是否为管理员的密令
    private final static String ADMIN_ORDER = "HCODE";

    // 管理在线用户，持有在线用户列表
    private final static ConcurrentHashMap<Channel, String> userMap = new ConcurrentHashMap<>();

    // 屏蔽列表1 key:发起屏蔽者id，value：被屏蔽者id集合 我屏蔽的人
    private final static ConcurrentHashMap<String, CopyOnWriteArraySet<String>> myShieldMap = new ConcurrentHashMap<>();

    // 屏蔽列表2 key:被屏蔽者id，value：发起屏蔽者id集合  屏蔽我的人
    private final static ConcurrentHashMap<String, CopyOnWriteArraySet<String>> otherShieldMap = new ConcurrentHashMap<>();

    //统一管理通道
    private final static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

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

    //表示channel 处于活动状态, 提示 xx上线
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel(); //当前连接用户的通道
        System.out.println(ctx.channel().remoteAddress() + " 上线了~");
    }

    //表示channel 处于不活动状态, 提示 xx离线了
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx.channel().remoteAddress() + " 离线了~");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MsgModel msgModel = (MsgModel) msg;
        Channel channel = ctx.channel();
        switch (msgModel.getType()) { //1. 管理员特权消息 2. 用户或管理员聊天消息 3.系统消息（客户端处理）
            case 1:
                AttributeKey<String> findKey = AttributeKey.valueOf(channel.id().asLongText());
                if (channel.attr(findKey).get().equals("admin")) {
                    handlerAdmin(ctx, msgModel);
                } else {
                    channel.writeAndFlush(new MsgModel((byte) 3, (byte) 6,
                            "<msg>错误：权限认证失败，未能识别您为管理员，当前操作失败！请重启重新认证！！！</msg><time>"
                                    + format(new Date()) + "</time>"));
                }
                break;
            case 2:
                handlerUser(ctx, msgModel);
                break;
            default:
                System.out.println("消息类型错误！！！");

        }


    }


    protected void handlerUser(ChannelHandlerContext ctx, MsgModel msg) {
        //获取到当前channel
        Channel channel = ctx.channel();
        switch (msg.getCode()) { //0.获取用户的昵称 1. 群聊 2.私聊 3.屏蔽某人 4.取消屏蔽 5.踢出某人 6.系统通知 7.管理员发布通知 8.心跳事件
            case 0: //获取用户的昵称，修改在线列表，并将当前在线用户的列表发给客户端,同时告知在线用户有人加入
                userMap.put(channel, msg.getBody()); //给在线用户设置对应的昵称
                MsgModel userList = new MsgModel((byte) 3, (byte) 9, getOnlineUserList(channel, msg.getBody()));
                channel.writeAndFlush(userList);
                AttributeKey<String> findKey = AttributeKey.valueOf(channel.id().asLongText());

                String sendMsg = "<id>" + channel.id().asLongText() +
                        "</id><name>" + msg.getBody() + "</name><authority>" +
                        channel.attr(findKey).get() + "</authority><time>" + format(new Date()) + "</time>";


                //将该客户加入聊天的信息推送给所有在线的客户端
                MsgModel notice = new MsgModel((byte) 3, (byte) 10, sendMsg);
                channelGroup.writeAndFlush(notice);

                break;
            case 1:     //群聊
                groupChat(channel, msg.getBody());
                break;
            case 2:     // 私聊相关操作
                privateChat(channel, msg);
                break;
            case 3:     // 屏蔽某人
                Pattern shieldPattern = Pattern.compile("<me>(.*)</me><other>(.*)</other>");
                Matcher shieldMatcher = shieldPattern.matcher(msg.getBody());
                if (shieldMatcher.find()) {
                    shieldUser(shieldMatcher.group(1), shieldMatcher.group(2));
                }
                break;
            case 4:     // 取消屏蔽某人
                Pattern unShieldPattern = Pattern.compile("<me>(.*)</me><other>(.*)</other>");
                Matcher unShieldMatcher = unShieldPattern.matcher(msg.getBody());
                if (unShieldMatcher.find()) {
                    unShieldUser(unShieldMatcher.group(1), unShieldMatcher.group(2));
                }
                break;
            case 8:    // 接受客户端发来的ping心跳，返回pong心跳告诉客户端没问题我知道你还活着。
                MsgModel msgModel = new MsgModel((byte) 3, (byte) 8, "A 'pong' beat heart from server to client");
                ctx.writeAndFlush(msgModel);
                break;
            case 110: // 权限认证
                AttributeKey<String> key = AttributeKey.valueOf(channel.id().asLongText());
                if (msg.getBody().equals(ADMIN_ORDER)) {
                    channel.attr(key).set("admin"); //设置该用户的权限为管理员
                } else {
                    channel.attr(key).set("user"); //设置该用户的权限为普通用户
                }
                break;
            default:
                System.out.println("用户消息指令有误~~");
        }
    }

    protected void handlerAdmin(ChannelHandlerContext ctx, MsgModel msg) {
        //获取到当前channel
        Channel channel = ctx.channel();
        AttributeKey<String> key = AttributeKey.valueOf(channel.id().asLongText());
        if (!channel.attr(key).get().equals("admin")) {
            String error = "<msg>错误：管理员权限未成功验证！您无权执行次操作！</msg><time>" + format(new Date()) + "</time>";
            channel.writeAndFlush(new MsgModel((byte) 3, (byte) 6, error));
            return;
        }
        switch (msg.getCode()) { //1. 群聊 2.私聊 3.屏蔽某人 4.取消屏蔽 5.踢出某人 6.系统通知 7.管理员发布通知
            case 5:     // 踢出某人
                String kickMsg = "<admin>" + channel.id().asLongText() + "</admin><kick>"+msg.getBody()+"</kick><time>" + format(new Date()) + "</time>";
                MsgModel sysMsg = new MsgModel((byte) 3, (byte) 5, kickMsg);
                channelGroup.forEach(ch -> {
                    ch.writeAndFlush(sysMsg);
                    if (ch.id().asLongText().equals(msg.getBody())) {
                        channelGroup.remove(ch);
                        userMap.remove(ch);
                        //将我的屏蔽列表清空
                        if (!myShieldMap.get(ch.id().asLongText()).isEmpty()) {
                            myShieldMap.remove(ch.id().asLongText());
                        }
                        //将我的被屏蔽列表清空
                        if (!otherShieldMap.get(channel.id().asLongText()).isEmpty()) {
                            otherShieldMap.remove(channel.id().asLongText());
                        }
                        if (ch.isActive()) {
                            ch.close();
                        }
                    }
                });
                break;
            case 7: //管理员发布系统消息
                String adminMsg = "<admin>" + channel.id().asLongText() + "</admin><msg>"+msg.getBody()+"</msg><time>" + format(new Date()) + "</time>";
                MsgModel adminMsgModel = new MsgModel((byte) 3, (byte) 7, adminMsg);
                channelGroup.writeAndFlush(adminMsgModel);
            default:
                System.out.println("管理员消息指令有误~~");
        }
    }

    protected String getOnlineUserList(Channel channel, String currentUsername) {
        StringBuffer userList;
        userList = new StringBuffer();
        AttributeKey<String> findKey = AttributeKey.valueOf(channel.id().asLongText());
        /*获得房间中所有的用户的列表，然后构造成一定的格式发送回去*/
        userList.append("<user><id>")
                .append(channel.id().asLongText())
                .append("</id><name>")
                .append(currentUsername)
                .append("</name><authority>")
                .append(channel.attr(findKey).get())
                .append("</authority></user>");
        for (Channel each : userMap.keySet()) {
            if (each != channel) {
                AttributeKey<String> tempKey = AttributeKey.valueOf(each.id().asLongText());
                userList.append("<user><id>")
                        .append(each.id().asLongText())
                        .append("</id><name>")
                        .append(userMap.get(each))
                        .append("</name><authority>")
                        .append(each.attr(tempKey).get())
                        .append("</authority></user>");
            }
        }
        return userList.toString();
    }

    protected void groupChat(Channel channel, String msg) {

        Pattern pattern = Pattern.compile("<id>(.*)</id><msg>(.*)</msg>");
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            String id = matcher.group(1);
            String sendMsg = matcher.group(2);
            if (id.equals(channel.id().asLongText())) {
                String chatMsg = "<id>" + id + "</id><name>" + userMap.get(channel) +
                        "</name><msg>" + sendMsg + "</msg>" + "<time>" + format(new Date()) + "</time>";
                MsgModel chatModel = new MsgModel((byte) 2, (byte) 1, chatMsg);
                String myId = channel.id().asLongText();
                channelGroup.forEach(ch -> { //必须我的屏蔽列表没他，我的被屏蔽列表也没他才能将信息发给他的通道
                    CopyOnWriteArraySet<String> myShieldSet = myShieldMap.get(myId);
                    CopyOnWriteArraySet<String> otherShieldSet = otherShieldMap.get(myId);
                    if (!myShieldSet.contains(ch.id().asLongText()) && !otherShieldSet.contains(ch.id().asLongText())) {
                        ch.writeAndFlush(chatModel);
                    }

                });
            }
        }
    }

    protected void privateChat(Channel channel, MsgModel msgModel) {
        Pattern pattern = Pattern.compile("<id>(.*)</id><order>(.*)</order><msg>(.*)</msg>");
        Matcher matcher = pattern.matcher(msgModel.getBody());
        if (matcher.find()) {
            AtomicBoolean flag = new AtomicBoolean(false);
            String otherId = matcher.group(1);
            String order = matcher.group(2);
            String getMsg = matcher.group(3);
            String myId = channel.id().asLongText();
            channelGroup.forEach(ch -> {
                if (ch.id().asLongText().equals(otherId) && ch.isActive()) {
                    flag.set(true);
                    switch (order) {
                        case "1":
                            if (!myShieldMap.get(myId).contains(ch.id().asLongText()) &&
                                    !otherShieldMap.get(myId).contains(ch.id().asLongText())
                            ) {
                                String startPrivate = "<id>" + myId + "</id><order>1</order>" +
                                        "<msg>" + getMsg + "</msg><time>" + format(new Date()) + "</time>";
                                ch.writeAndFlush(new MsgModel((byte) 2, (byte) 2, startPrivate));
                            } else {
                                channel.writeAndFlush(new MsgModel((byte) 3, (byte) 6,
                                        "<msg>错误：你屏蔽了对方或者对方屏蔽了你，无法进行私聊！</msg><time>"
                                                + format(new Date()) + "</time>"));
                            }
                            break;
                        case "2":
                            String endPrivate = "<id>" + myId + "</id><order>2</order>" +
                                    "<msg>" + getMsg + "</msg><time>" + format(new Date()) + "</time>";
                            ch.writeAndFlush(new MsgModel((byte) 2, (byte) 2, endPrivate));
                            break;
                        case "3":
                            String senMsg = "<id>" + myId + "</id><order>3</order>" +
                                    "<msg>" + getMsg + "</msg><time>" + format(new Date()) + "</time>";
                            ch.writeAndFlush(new MsgModel((byte) 2, (byte) 2, senMsg));
                            break;
                        default:
                            System.out.println("私聊指令有误~");
                    }
                }else if (ch.id().asLongText().equals(otherId) && !ch.isActive()){
                    flag.set(true);
                    String senMsg = "<id>" + otherId + "</id><order>4</order>" +
                            "<msg>错误！发送该消息失败！由于对方已不在聊天室，此次私聊结束！</msg><time>" + format(new Date()) + "</time>";
                    channel.writeAndFlush(new MsgModel((byte) 2, (byte) 4, senMsg));
                }
            });

            if (!flag.get()) { //如果该用户被移除了，可能是非正常离线
                String senMsg = "<id>" + otherId + "</id><order>4</order>" +
                        "<msg>错误！发送该消息失败！由于对方已不在聊天室，此次私聊结束！</msg><time>" + format(new Date()) + "</time>";
                channel.writeAndFlush(new MsgModel((byte) 2, (byte) 4, senMsg));
            }

        }
    }


    protected void shieldUser(String myId, String shieldId) {
        // 加入我的屏蔽列表
        if (!myShieldMap.containsKey(myId)) {
            CopyOnWriteArraySet<String> myShieldList = new CopyOnWriteArraySet<>();
            myShieldList.add(shieldId);
            myShieldMap.put(myId, myShieldList);
        } else {
            myShieldMap.get(myId).add(shieldId);
        }
        // 在对方被屏蔽列表中加入我
        if (!otherShieldMap.containsKey(shieldId)) {
            CopyOnWriteArraySet<String> shieldMeList = new CopyOnWriteArraySet<>();
            shieldMeList.add(myId);
            otherShieldMap.put(shieldId, shieldMeList);
        } else {
            otherShieldMap.get(shieldId).add(myId);
        }
    }

    protected void unShieldUser(String myId, String unShieldId) {
        // 从我的屏蔽列表移除他
        if (myShieldMap.containsKey(myId)) {
            myShieldMap.get(myId).remove(unShieldId);
        }
        // 从他的被屏蔽列表移除我
        if (otherShieldMap.containsKey(unShieldId)) {
            otherShieldMap.get(unShieldId).remove(myId);
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
        super.channelReadComplete(ctx);
    }


    /**
     * @MethodName userEventTriggered
     * @Params * @param null
     * @Description 每隔12s对客服端进行心跳检测，客户端未反应，表示客户端已出问题，关闭通道。
     * @Return
     * @Since 2020/7/7
     */

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                System.out.println("该用户已超过心跳检测时间，未做出反应，关闭通道...");
                ctx.close();
                channelGroup.forEach(ch -> {
                    AttributeKey<String> findKey = AttributeKey.valueOf(ch.id().asLongText());
                    if (ch.attr(findKey).get().equals("admin")){  // 通知管理员
                        ch.writeAndFlush(new MsgModel((byte) 3, (byte) 6,
                                "<msg>ip为"+ctx.channel().remoteAddress()+
                                        " 的用户["+userMap.get(ctx.channel())+"]上超过心跳检测的时间，系统强制关闭其通道。</time>"
                                        + format(new Date()) + "</time>"));
                    }
                });
            }
        }
    }

    /**
     * @MethodName exceptionCaught
     * @Params * @param null
     * @Description 发生异常，直接关闭该通道
     * @Return
     * @Since 2020/7/7
     */

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println(cause.getMessage());
        if (ctx.channel().isActive()) {
            ctx.close();
        }
    }


    //handlerAdded 表示连接建立，一旦连接，第一个被执行
    //将当前channel 加入到  channelGroup
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        channelGroup.add(channel);
        CopyOnWriteArraySet<String> myShieldList = new CopyOnWriteArraySet<>();
        CopyOnWriteArraySet<String> otherShieldMeList = new CopyOnWriteArraySet<>();
        myShieldMap.put(channel.id().asLongText(), myShieldList);
        otherShieldMap.put(channel.id().asLongText(), otherShieldMeList);
        userMap.put(channel, "未知"); //第一次加入未知昵称，需等客户端将昵称发过来
    }

    //断开连接, 将xx客户离开信息推送给当前在线的客户
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        //将我的屏蔽列表清空
        if (!myShieldMap.get(channel.id().asLongText()).isEmpty()) {
            myShieldMap.remove(channel.id().asLongText());
        }
        //将我的被屏蔽列表清空
        if (!otherShieldMap.get(channel.id().asLongText()).isEmpty()) {
            otherShieldMap.remove(channel.id().asLongText());
        }


        AttributeKey<String> findKey = AttributeKey.valueOf(channel.id().asLongText());
        String sendMsg = "<id>" + channel.id().asLongText() +
                "</id><name>" + userMap.get(channel) + "</name><authority>" +
                channel.attr(findKey).get() + "</authority><time>" + format(new Date()) + "</time>";

        //将该客户退出聊天的信息推送给所有在线的客户端
        MsgModel notice = new MsgModel((byte) 3, (byte) 11, sendMsg);
        channelGroup.writeAndFlush(notice);
        userMap.remove(channel);
    }


}
