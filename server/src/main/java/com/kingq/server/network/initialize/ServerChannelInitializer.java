package com.kingq.server.network.initialize;

import com.kingq.server.KingQServer;
import com.kingq.server.network.Connection;
import com.kingq.shared.pipeline.handler.JSONObjectDecoder;
import com.kingq.shared.pipeline.handler.JSONObjectEncoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Created by spring on 07.10.2017.
 */
public class ServerChannelInitializer extends ChannelInitializer<Channel> {

    private KingQServer kingQServer;

    public ServerChannelInitializer(KingQServer kingQServer) {

        this.kingQServer = kingQServer;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {

        try {
            channel.config().setOption(ChannelOption.IP_TOS, 0x18);
        } catch (ChannelException e) {
            // Not supported
        }
        channel.config().setAllocator(PooledByteBufAllocator.DEFAULT);

        channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4));
        channel.pipeline().addLast(new JSONObjectDecoder());
        channel.pipeline().addLast(new LengthFieldPrepender(4));
        channel.pipeline().addLast(new JSONObjectEncoder());
        channel.pipeline().addLast(new Connection(kingQServer, channel));
    }
}
