package top.hcode.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import top.hcode.commons.MsgModel;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/9 20:22
 * @Description:
 */
public class ChatRoomClientHandler extends ChannelInboundHandlerAdapter {
    private ClientFrame clientFrame;

    public ChatRoomClientHandler(ClientFrame clientFrame) {
        this.clientFrame = clientFrame;
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 昵称初始化
        MsgModel nameMsg = new MsgModel((byte)2, (byte)0, ChatRoomClient.name);

        // 权限认证
        MsgModel msgModel = new MsgModel((byte)2, (byte)110, ChatRoomClient.order);

        ctx.channel().writeAndFlush(msgModel);
        ctx.channel().writeAndFlush(nameMsg);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MsgModel msgModel = (MsgModel)msg;
        switch(msgModel.getType()) {
            case 2: //处理用户或管理员相关消息
                this.handlerUserMsg(ctx, msgModel);
                break;
            case 3: // 系统消息
                this.handlerSysMsg(ctx, msgModel);
                break;
            default:
                System.out.println("消息类型错误！！！");
        }

    }

    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent)evt;
            switch(e.state()) {
                case READER_IDLE:
                    ctx.close();
                case WRITER_IDLE: //客户端发生写空闲，感觉发送心跳给服务端，告知服务端我还活着，防止被断开连接
                    this.SendPingToServer(ctx);
            }
        }

    }

    //发送心跳给服务端
    protected void SendPingToServer(ChannelHandlerContext ctx) {
        MsgModel msgModel = new MsgModel((byte)2, (byte)8, "a ‘ping’ beat heart from"+ctx.channel().remoteAddress());
        ctx.writeAndFlush(msgModel);
    }


    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }


    public void handlerUserMsg(ChannelHandlerContext ctx, MsgModel msg) {
        switch(msg.getCode()) {
            case 1: //处理群聊消息
                this.clientFrame.updateTextArea(msg.getBody(), "user");
                break;
            case 2: // 处理私聊相关操作
                this.clientFrame.AllPrivateChatActions(msg.getBody());
                break;
            default:
                System.out.println("用户指令类型错误！！！");
        }

    }

    public void handlerSysMsg(ChannelHandlerContext ctx, MsgModel msg) {
        switch(msg.getCode()) {
            case 5: // 被踢消息处理 可能别人被踢也可能自己被踢
                this.clientFrame.beKick(msg.getBody());
                break;
            case 6: // 更新系统消息
                this.clientFrame.updateTextArea(msg.getBody(), "sys");
                break;
            case 7:  // 更新管理员发布的系统消息
                this.clientFrame.updateTextArea(msg.getBody(), "admin");
            case 8:
                break;
            case 9:  // 初始化获取当前在线的用户
                this.clientFrame.getUserList(msg.getBody());
                break;
            case 10: // 有新用户加入的更新操作
                this.clientFrame.addUser(msg.getBody());
                break;
            case 11: // 有用户退出聊天室的删除操作
                this.clientFrame.delUser(msg.getBody());
                break;
            default:
                System.out.println("系统指令类型错误！！！");
        }

    }
}