package top.hcode.server;


import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;
import top.hcode.commons.MsgPackDecoder;
import top.hcode.commons.MsgPackEncoder;


/**
 * @Params
 * @Description 管道pipeline初始化，将处理器handler加入到管道里面
 * @Return
 * @Since 2020/7/11
 */

public class ChatRoomServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        /**
         *
         * @param maxFrameLength  帧的最大长度
         * @param lengthFieldOffset length字段偏移的地址
         * @param lengthFieldLength length字段所占的字节长
         * @param lengthAdjustment 修改帧数据长度字段中定义的值，可以为负数 因为有时候我们习惯把头部记入长度,若为负数,则说明要推后多少个字段
         * @param initialBytesToStrip 解析时候跳过多少个长度
         * @param failFast 为true，当frame长度超过maxFrameLength时立即报TooLongFrameException异常，为false，读取完整个帧再报异
         */
        pipeline.addLast(new IdleStateHandler(20,10,0));
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(65535, 0, 2, 0, 2, false));

        pipeline.addLast("MsgPackDecoder", new MsgPackDecoder());//解码器

        /* @param lengthFieldLength length字段所占的字节长*/
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(2));

        pipeline.addLast("MsgPackEncoder", new MsgPackEncoder());//编码器

        pipeline.addLast("ServerHandler", new ChatRoomServerHandler());//自定义业务处理器

    }
}
