package top.hcode.server;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/7 14:58
 * @Description:
 */
public class ChatRoomServerBootstrap {
    public static void main(String[] args) {
        new ChatRoomServer().start(8888);
    }
}