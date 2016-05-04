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

package de.jackwhite20.japs.client.pub.impl;

import com.google.gson.Gson;
import de.jackwhite20.japs.client.pub.Publisher;
import de.jackwhite20.japs.client.pub.impl.exception.PublisherConnectException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class PublisherImpl implements Publisher {

    private boolean connected;

    private String host;

    private int port;

    private SocketChannel socketChannel;

    private Gson gson = new Gson();

    public PublisherImpl(String host, int port) {

        this.host = host;
        this.port = port;

        // Connect
        connect();
    }

    private void connect() {

        try {
            // Open a new socket channel and connect to the host and port
            socketChannel = SocketChannel.open();
            // Disable the Nagle algorithm
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.connect(new InetSocketAddress(host, port));

            // We are connected successfully
            connected = true;
        } catch (IOException e) {
            throw new PublisherConnectException("error while connecting to " + host + ":" + port);
        }
    }

    @Override
    public void disconnect() {

        // We are not connected anymore
        connected = false;

        // If not null disconnect the socket channel
        if(socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void publish(String channel, JSONObject jsonObject) {

        publish(channel, jsonObject, null);
    }

    @Override
    public void publish(String channel, JSONObject jsonObject, String subscriberName) {

        if(channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("channel cannot be null or empty");
        }

        if(jsonObject == null || jsonObject.length() == 0) {
            throw new IllegalArgumentException("jsonObject cannot be null or empty");
        }

        // Set the op code, channel
        jsonObject.put("op", 2);
        jsonObject.put("ch", channel);
        // Set the subscriber name if it is not null
        if(subscriberName != null) {
            jsonObject.put("su", subscriberName);
        }

        try {
            // Send the JSONObject as JSON string with a line break at the end
            socketChannel.write(ByteBuffer.wrap((jsonObject.toString() + "\n").getBytes()));
        } catch (IOException e) {
            disconnect();
            e.printStackTrace();
        }
    }

    @Override
    public void publish(String channel, String json) {

        publish(channel, json, null);
    }

    @Override
    public void publish(String channel, String json, String subscriberName) {

        if(json == null || json.isEmpty()) {
            throw new IllegalArgumentException("json cannot be null or empty");
        }

        try {
            // Publish it as JSONObject that we can add the op code and channel
            publish(channel, new JSONObject(json), subscriberName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(String channel, Object object) {

        publish(channel, object, null);
    }

    @Override
    public void publish(String channel, Object object, String subscriberName) {

        if(object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }

        // Publish the serialized object as json string
        publish(channel, gson.toJson(object), subscriberName);
    }

    @Override
    public boolean connected() {

        return connected;
    }
}
