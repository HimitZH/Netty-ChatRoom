package top.hcode.server;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/7 14:58
 * @Description: 服务端总启动器，设置服务端口。
 */
public class ChatRoomServerBootstrap {
    public static void main(String[] args) {
        new ChatRoomServer().start(8888);
    }
}