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

package de.jackwhite20.japs.shared.pipeline.handler;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;
import org.json.JSONObject;

import java.nio.CharBuffer;
import java.util.List;

/**
 * Created by JackWhite20 on 07.10.2016.
 */
public class JSONObjectEncoder extends MessageToMessageEncoder<JSONObject> {

    @Override
    protected void encode(ChannelHandlerContext ctx, JSONObject jsonObject, List<Object> out) throws Exception {

        out.add(ByteBufUtil.encodeString(ctx.alloc(), CharBuffer.wrap(jsonObject.toString()), CharsetUtil.UTF_8));
    }
}
