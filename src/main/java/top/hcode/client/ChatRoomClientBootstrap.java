package top.hcode.client;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 17:45
 * @Description: 聊天室客户端总启动器
 */
public class ChatRoomClientBootstrap {
    public static void main(String[] args) {
        ChatRoomClient.chatRoomClient = new ChatRoomClient();
        ChatRoomClient.login = new Login();
    }
}