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

package de.jackwhite20.japs.server.network;

import de.jackwhite20.japs.server.JaPS;
import de.jackwhite20.japs.server.JaPSServer;
import de.jackwhite20.japs.shared.net.OpCode;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class Connection {

    private static final Logger LOGGER = JaPS.getLogger();

    private static final int BUFFER_GROW_FACTOR = 2;

    private boolean connected = true;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

    private JaPSServer server;

    private SocketChannel socketChannel;

    private List<String> channels = new ArrayList<>();

    private SocketAddress remoteAddress;

    private String host;

    private int port;

    private String name;

    public Connection(JaPSServer server, SocketChannel socketChannel) {

        this.server = server;
        this.socketChannel = socketChannel;
        try {
            this.remoteAddress = socketChannel.getRemoteAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String data) {

        try {
            byte[] bytes = data.getBytes("UTF-8");
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
            byteBuffer.putInt(bytes.length);
            byteBuffer.put(bytes);

            byteBuffer.flip();

            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            // Close the connection due to broken pipe
            close();
        }
    }

    private void close() {

        if (!connected) {
            return;
        }

        connected = false;

        server.removeClient(this);

        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LOGGER.log(Level.FINE, "[{0}] Connection closed", remoteAddress.toString());
    }

    public void read() {

        byteBuffer.limit(byteBuffer.capacity());

        try {
            int read = socketChannel.read(byteBuffer);

            // Read is -1 if connection is closed
            if (read == -1) {
                close();
                return;
            }

            // Resize buffer and read the rest if the buffer was too small
            if (byteBuffer.remaining() == 0) {
                ByteBuffer temp = ByteBuffer.allocate(byteBuffer.capacity() * BUFFER_GROW_FACTOR);
                byteBuffer.flip();
                temp.put(byteBuffer);
                byteBuffer = temp;

                int position = byteBuffer.position();
                byteBuffer.flip();
                // Reset to last position (the position from the half read packet) after flip
                byteBuffer.position(position);

                // Read again to read the left packet
                read();
            }

            byteBuffer.flip();

            while (byteBuffer.remaining() > 0) {
                byteBuffer.mark();

                if (byteBuffer.remaining() < 4) {
                    break;
                }

                int readable = byteBuffer.getInt();
                if (byteBuffer.remaining() < readable) {
                    byteBuffer.reset();
                    break;
                }

                byte[] bytes = new byte[readable];
                byteBuffer.get(bytes);

                String json = new String(bytes, "UTF-8");

                JSONObject jsonObject = new JSONObject(json);

                if (jsonObject.isNull("op")) {
                    break;
                }

                int op = ((Integer) jsonObject.remove("op"));

                OpCode opCode = OpCode.of(op);

                switch (opCode) {
                    case OP_BROADCAST:
                        if (!jsonObject.has("su")) {
                            // Broadcast it to all subscriber
                            server.broadcast(this, jsonObject.getString("ch"), jsonObject.toString());
                        } else {
                            // Broadcast to specific subscriber
                            server.broadcastTo(this, jsonObject.getString("ch"), jsonObject, jsonObject.getString("su"));
                        }
                        break;
                    case OP_CACHE_GET:
                        String getKey = jsonObject.getString("key");
                        int getCallbackId = jsonObject.getInt("id");

                        Object getValue = server.cache().get(getKey);

                        JSONObject getResponse = new JSONObject()
                                .put("op", 5)
                                .put("id", getCallbackId)
                                .put("value", getValue);

                        send(getResponse.toString());

                        LOGGER.log(Level.FINE, "[{0}] Got cache entry {1}={2} and a callback id of {3}", new Object[] {remoteAddress.toString(), getKey, getValue, getCallbackId});
                        break;
                    case OP_CACHE_ADD:
                        String key = jsonObject.getString("key");
                        Object value = jsonObject.get("value");
                        int expire = jsonObject.getInt("expire");

                        server.cache().put(key, value, expire);

                        server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_ADD.getCode()));

                        LOGGER.log(Level.FINE, "[{0}] Added cache entry {1}={2} with an expire of {3}", new Object[] {remoteAddress.toString(), key, value, expire});
                        break;
                    case OP_CACHE_REMOVE:
                        String removeKey = jsonObject.getString("key");

                        server.cache().remove(removeKey);

                        server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_REMOVE.getCode()));

                        LOGGER.log(Level.FINE, "[{0}] Removed cache entry with key {1}", new Object[] {remoteAddress.toString(), removeKey});
                        break;
                    case OP_CACHE_HAS:
                        boolean has = server.cache().has(jsonObject.getString("key"));

                        JSONObject hasResponse = new JSONObject()
                                .put("op", OpCode.OP_CACHE_HAS.getCode())
                                .put("id", jsonObject.getInt("id"))
                                .put("has", has);

                        send(hasResponse.toString());
                        break;
                    case OP_CACHE_SET_EXPIRE:
                        String expireKey = jsonObject.getString("key");
                        int expireSeconds = jsonObject.getInt("expire");

                        server.cache().expire(expireKey, expireSeconds);

                        server.clusterBroadcast(this, jsonObject.put("op", OpCode.OP_CACHE_SET_EXPIRE.getCode()));

                        LOGGER.log(Level.FINE, "[{0}] Set expire seconds for key {1} to {2} seconds", new Object[] {remoteAddress.toString(), expireKey, expireSeconds});
                        break;
                    case OP_CACHE_GET_EXPIRE:
                        String expireGetKey = jsonObject.getString("key");
                        int expireGetCallbackId = jsonObject.getInt("id");

                        int expireGetValue = ((int) server.cache().expire(expireGetKey));

                        JSONObject expireGetResponse = new JSONObject()
                                .put("op", 5)
                                .put("id", expireGetCallbackId)
                                .put("value", expireGetValue);

                        send(expireGetResponse.toString());

                        LOGGER.log(Level.FINE, "[{0}] Got expire in time for key {1} which will expire in {2} seconds", new Object[] {remoteAddress.toString(), expireGetKey, expireGetValue});
                        break;
                    case OP_REGISTER_CHANNEL:
                        String channelToRegister = jsonObject.getString("ch");

                        server.subscribeChannel(channelToRegister, this);
                        channels.add(channelToRegister);
                        break;
                    case OP_UNREGISTER_CHANNEL:
                        String channelToRemove = jsonObject.getString("ch");

                        server.unsubscribeChannel(channelToRemove, this);
                        channels.remove(channelToRemove);
                        break;
                    case OP_SUBSCRIBER_SET_NAME:
                        name = jsonObject.getString("su");

                        LOGGER.log(Level.FINE, "[{0}] Subscriber name set to: {1}", new Object[]{remoteAddress.toString(), name});
                        break;
                    case OP_CLUSTER_INFO_SET:
                        host = jsonObject.getString("host");
                        port = jsonObject.getInt("port");

                        LOGGER.log(Level.FINE, "[{0}] Cluster info set to: {1}:{2}", new Object[]{remoteAddress.toString(), host, String.valueOf(port)});
                        break;
                    case OP_KEEP_ALIVE:
                        // Ignore for now
                        //LOGGER.log(Level.FINE, "[{0}] Keep alive time: {1}", new Object[]{remoteAddress.toString(), System.currentTimeMillis()});
                        break;
                    case OP_UNKNOWN:
                        LOGGER.log(Level.WARNING, "[{0}] Unknown OP code received: {0}", new Object[]{remoteAddress.toString(), op});
                        break;
                }
            }

            byteBuffer.compact();
        } catch (IOException e) {
            close();
        }
    }

    public List<String> channels() {

        return channels;
    }

    public SocketAddress remoteAddress() {

        return remoteAddress;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    public String name() {

        return name;
    }

    public boolean connected() {

        return socketChannel.isConnected() && connected;
    }
}
