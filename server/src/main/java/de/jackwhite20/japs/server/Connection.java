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

package de.jackwhite20.japs.server;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
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

    private static final Logger LOGGER = Logger.getLogger(Connection.class.getName());

    private static final int INITIAL_STRING_BUILDER_SIZE = 256;

    private static final int BUFFER_GROW_FACTOR = 2;

    private boolean connected = true;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

    private JaPSServer server;

    private SocketChannel socketChannel;

    private List<String> channels = new ArrayList<>();

    private StringBuilder stringBuilder = new StringBuilder(INITIAL_STRING_BUILDER_SIZE);

    private SocketAddress remoteAddress;

    private String host;

    private int port;

    private String name;

    public Connection(JaPSServer server, SocketChannel socketChannel) {

        this.server = server;
        this.socketChannel = socketChannel;
        try {
            this.remoteAddress = socketChannel.getRemoteAddress();
            host = ((InetSocketAddress) remoteAddress).getHostName();
            port = ((InetSocketAddress) remoteAddress).getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(String data) {

        try {
            socketChannel.write(ByteBuffer.wrap((data + "\n").getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {

        if(!connected) {
            return;
        }

        connected = false;

        if(socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        server.removeClient(this);

        LOGGER.log(Level.FINE, "[{0}] Connection closed", remoteAddress.toString());
    }

    public void read() {

        byteBuffer.limit(byteBuffer.capacity());

        try {
            int read = socketChannel.read(byteBuffer);

            // Read is -1 if connection is closed
            if(read == -1) {
                close();
                return;
            }

            // Resize buffer and read the rest if the buffer was too small
            if(byteBuffer.remaining() == 0) {
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

            byte[] bytes = new byte[read];
            byteBuffer.get(bytes);

            for (byte aByte : bytes) {
                char c = ((char) aByte);
                stringBuilder.append(c);

                if(c == '\n') {
                    String jsonLine = stringBuilder.toString();
                    stringBuilder.setLength(0);

                    JSONObject jsonObject = new JSONObject(jsonLine);

                    if(jsonObject.isNull("op")) {
                        break;
                    }

                    int op = ((Integer) jsonObject.remove("op"));

                    switch (op) {
                        case 2:
                            if(!jsonObject.has("su")) {
                                // Broadcast it to all subscriber
                                server.broadcast(this, jsonObject.getString("ch"), jsonObject.toString());
                            } else {
                                // Broadcast to specific subscriber
                                server.broadcastTo(this, jsonObject.getString("ch"), jsonObject.toString(), jsonObject.getString("su"));
                            }
                            break;
                        case 0:
                            String channelToRegister = jsonObject.getString("ch");

                            server.subscribeChannel(channelToRegister, this);
                            channels.add(channelToRegister);
                            break;
                        case 1:
                            String channelToRemove = jsonObject.getString("ch");

                            server.unsubscribeChannel(channelToRemove, this);
                            channels.remove(channelToRemove);
                            break;
                        case 3:
                            name = jsonObject.getString("su");

                            LOGGER.log(Level.FINE, "[{0}] Subscriber name set to: {1}", new Object[] {remoteAddress.toString(), name});
                            break;
                        default:
                            LOGGER.log(Level.WARNING, "[{0}] Unknown OP code received: {0}", new Object[] {remoteAddress.toString(), op});
                    }
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
}
