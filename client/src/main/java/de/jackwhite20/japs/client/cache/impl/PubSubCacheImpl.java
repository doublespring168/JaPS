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

package de.jackwhite20.japs.client.cache.impl;

import de.jackwhite20.japs.client.cache.PubSubCache;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by JackWhite20 on 13.06.2016.
 */
public class PubSubCacheImpl implements PubSubCache {

    private SocketChannel socketChannel;

    public PubSubCacheImpl(SocketChannel socketChannel) {

        this.socketChannel = socketChannel;
    }

    @Override
    public void put(String key, Object value, int expire) {

        JSONObject jsonObject = new JSONObject()
                .put("op", 4)
                .put("key", key)
                .put("value", value)
                .put("expire", expire);

        try {
            // Send the json data and prepend the length
            byte[] bytes = jsonObject.toString().getBytes("UTF-8");
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
            byteBuffer.putInt(bytes.length);
            byteBuffer.put(bytes);

            // Prepare the buffer for writing
            byteBuffer.flip();

            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            // TODO: 13.06.2016 Will be changed anyway; just testing
        }
    }

    @Override
    public void put(String key, Object value) {

        put(key, value, -1);
    }
}
