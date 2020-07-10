package top.hcode.commons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.msgpack.annotation.Message;

import java.io.Serializable;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/6 23:28
 * @Description:
 */
@Message
@NoArgsConstructor
@AllArgsConstructor
@Data
@Accessors
public class MsgModel implements Serializable {
    private byte type; // 权限信息标志  1. 管理员特权消息 2. 用户或管理员消息 3.系统消息（客户端处理）
    private byte code; // 命令符
    // 0.上线消息 1. 群聊 2.私聊 3.屏蔽某人 4.取消屏蔽 5.踢出某人
    // 6.系统通知 7.管理员发布通知 8. 心跳 9.发送在线用户列表给客户端 10.新用户加入 11.用户退出 110.权限认证
    private String body; // 消息主体
    /*

    code 命令符的含义如下：

        1.群聊的body格式
        客户端发送： <id>${socketChannel.id}</id><msg>${msg}</msg>
        服务端发送： <id>${socketChannel.id}</id><name>${name}</name><msg>${msg}</msg><time>${time}</time>

        2.私聊的body格式为
        客户端发送：<id>${socketChannel.id}</id><order>{orderNum}</order><msg>${msg}</msg>

        当order为1，2时，msg为start exit

        服务端发送：<id>${socketChannel.id}</id><order>${orderNum}</order><msg>${msg}</msg><time>${time}</time>
        orderNum---> 1.发起私聊  2.结束私聊 3.普通私聊信息 4.对方已离线，关闭私聊窗口

        3 4.屏蔽或取消屏蔽的body格式为
        客户端发送：<me>${myId}</me><other>${shieldId}</other>

        5.管理员踢人指令格式：
         客户端发送：发生所选用户的id
         服务端发送：<admin>${socketChannel.id}</admin><kick>${beKick.id}</kick><time>${time}</time>

        6.系统通知消息的body格式为：
         <msg>${msg}</msg><time>${time}</time>

        7.管理员发布系统消息格式：
         <id>${socketChannel.id}</id><msg>${msg}</msg><time>${time}</time>

        8. 心跳事件：
         客户端发送："a ‘ping’ beat heart from"+ctx.channel().remoteAddress() //后面为该用户端所在的ip和端口号
         服务端：只进行心跳检测，客户端在心跳检测事件内有发送信息让服务端读到，则说明该用户还存活，否则关闭通道。

        9.用户列表 由服务器端查询在线用户发送给新加入的用户
         格式为：
         <user><id>${socketChannel.id}</id><name>${name}</name><authority>${admin or user}</authority></user>
         注：每个用户的信息都如上，然后列表有多个用户信息拼接而成。

        10 11.用户进入与退出：
        <id>${socketChannel.id}</id><name>${name}</name><authority>${authority}</authority><time>${time}</time>

        110.权限认证：
        客户端发送：管理员认证密钥HCODE即可行驶管理员权力。

     */
}