package top.hcode.client;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/10 17:45
 * @Description:
 */
public class ChatRoomClientBootstrap {
    public static void main(String[] args) {
        ChatRoomClient.chatRoomClient = new ChatRoomClient();
        ChatRoomClient.login = new Login();
    }
}