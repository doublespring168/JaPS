package com.kingq.shared.pipeline;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;

/**
 * Created by spring on 07.10.2017.
 */
public final class ChannelUtil {

    private ChannelUtil() {
        // no instance
    }

    public static void closeOnFlush(Channel ch) {

        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
