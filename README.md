# Netty-ChatRoom
使用Netty和Swing实现在线聊天室  
具有心跳检测，断线重连，实时监控是否断线，进行有限次数的重连操作！
# 功能:
## 服务端
服务端负责转发消息，执行管理员命令，验证权限等。
## 起始登录窗口（GUI界面）
可选择以普通用户或管理员身份进入，管理员需要验证密钥（默认为“HCODE”）
## 管理端（GUI界面）
> 1）管理员设置聊天室IP，端口号，管理员昵称，连接服务器进入聊天室或退出聊天室。  
> 2）系统消息日志记录，管理员可发布系统消息给各在线用户。  
> 3）管理员在线与聊天室在线用户进行群聊。   
> 4）管理员可对在线用户列表中指定用户进行私聊请求，对方同意即可开始私聊。  
> 5）管理员可对在线用户列表中指定用户进行踢出聊天室操作，并通知其他人。  

## 用户端（GUI界面）
> 1）用户设置聊天室IP，端口号，用户昵称，连接服务器进入聊天室或退出聊天室。  
> 2）系统消息通知，接受服务器端发布的消息，以及用户一些操作。  
> 3）用户可与其他在线用户进行群聊。  
> 4）用户可与指定用户列表中其他在线用户进行私聊请求，同意即可开始私聊。  
> 5）用户可以屏蔽指定用户列表中的用户的群聊发言，屏蔽后即接受不到对方发言， 同时也可以选择取消屏蔽。  
# 传输的指令参数如下
-  type：权限信息标志  
1. 管理员特权消息 
2. 用户或管理员消息 
3. 系统消息（客户端处理）
-  code：命令符 
0. 上线消息 1. 群聊 2. 私聊 3. 屏蔽某人 4. 取消屏蔽 5. 踢出某人 6. 系统通知 7. 管理员发布通知 8. 心跳 9. 发送在线用户列表给客户端 10. 新用户加入 11. 用户退出 110. 权限认证
-  code具体描述如下：
      
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

        8.心跳事件：
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

    
