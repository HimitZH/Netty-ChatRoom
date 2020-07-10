package top.hcode.commons;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/6 23:17
 * @Description:
 */
public class MsgPackEncoder extends MessageToByteEncoder {
    protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf byteBuf) throws Exception {
        MessagePack pack = new MessagePack();

        // 将对象编码为MessagePack格式的字节数组
        byte[] write = pack.write(msg);

        byteBuf.writeBytes(write);
    }
}