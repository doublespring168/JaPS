/*
 * Copyright (c) 2016 "JackWhite20"
 *
 * This file is part of JaPS.
 *
 * JaPS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.jackwhite20.japs.shared.pipeline.initialize;

import de.jackwhite20.japs.shared.nio.NioSocketClient;
import de.jackwhite20.japs.shared.pipeline.handler.JSONObjectDecoder;
import de.jackwhite20.japs.shared.pipeline.handler.JSONObjectEncoder;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

/**
 * Created by JackWhite20 on 07.10.2016.
 */
public class ClientChannelInitializer extends ChannelInitializer<Channel> {

    private NioSocketClient nioSocketClient;

    public ClientChannelInitializer(NioSocketClient nioSocketClient) {

        this.nioSocketClient = nioSocketClient;
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
        channel.pipeline().addLast(nioSocketClient);
    }
}
