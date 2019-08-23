package com.kingq.shared.pipeline.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import org.json.JSONObject;

import java.nio.CharBuffer;
import java.util.List;

/**
 * Created by spring on 07.10.2017.
 */
public class JSONObjectEncoder extends MessageToMessageEncoder<JSONObject> {

    @Override
    protected void encode(ChannelHandlerContext ctx, JSONObject jsonObject, List<Object> out) throws Exception {

        out.add(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(jsonObject.toString()), CharsetUtil.UTF_8));
    }
}
