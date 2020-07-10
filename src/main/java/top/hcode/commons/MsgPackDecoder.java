package top.hcode.commons;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;


/**
 * @Author: Himit_ZH
 * @Date: 2020/7/6 23:17
 * @Description:
 */
public class MsgPackDecoder extends MessageToMessageDecoder<ByteBuf> {

    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> list) throws Exception {

        int length = in.readableBytes();
        byte[] content = new byte[length];
        in.readBytes(content);

        //封装成 MessagePack 对象，放入 list， 传递下一个handler业务处理
        MessagePack messagePack = new MessagePack();

        list.add(messagePack.read(content, MsgModel.class));
    }
}