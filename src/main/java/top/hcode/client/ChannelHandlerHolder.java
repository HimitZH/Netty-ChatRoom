package top.hcode.client;

import io.netty.channel.ChannelHandler;

public interface ChannelHandlerHolder {

    ChannelHandler[] handlers();  // 客户端断连前所持有的处理器
}
