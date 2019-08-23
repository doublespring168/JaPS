package com.kingq.shared.pipeline.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by spring on 07.10.2017.
 */
public class JSONObjectDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {

        byteBuf.readInt();
        out.add(new JSONObject(byteBuf.toString(CharsetUtil.UTF_8)));
    }
}
