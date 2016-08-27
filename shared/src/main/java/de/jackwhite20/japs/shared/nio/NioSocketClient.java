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

package de.jackwhite20.japs.shared.nio;

import de.jackwhite20.japs.shared.config.ClusterServer;
import de.jackwhite20.japs.shared.net.ConnectException;
import de.jackwhite20.japs.shared.net.OpCode;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by JackWhite20 on 16.06.2016.
 */
public abstract class NioSocketClient implements Runnable {

    private static final long KEEP_ALIVE_TIME = 1000;

    private static final int CONNECT_TIMEOUT = 2000;

    private static final int BUFFER_SIZE = 4096;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private List<ClusterServer> clusterServers = new ArrayList<>();

    private SocketChannel socketChannel;

    private Selector selector;

    private int clusterServerIndex = 0;

    private long reconnectPause = 0;

    private boolean connected;

    private AtomicBoolean reconnecting = new AtomicBoolean(false);

    private Queue<String> sendQueue = new ConcurrentLinkedQueue<>();

    protected String name;

    public NioSocketClient(List<ClusterServer> clusterServers, String name) {

        // Randomize the list to give a chance for a better use of the cluster
        Collections.shuffle(clusterServers);

        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }

        this.clusterServers = clusterServers;
        this.name = name;

        ClusterServer first = clusterServers.get(0);
        if (!connect(first.host(), first.port())) {
            throw new ConnectException("cannot initially connect to " + first.host() + ":" + first.port());
        }
    }

    public NioSocketClient(List<ClusterServer> clusterServers) {

        this(clusterServers, "server");
    }

    public abstract void clientConnected();

    public abstract void clientReconnected();

    public abstract void received(JSONObject jsonObject);

    public boolean connect(String host, int port) {

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();

            // Disable the Nagle algorithm
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            // Try to connect to the host and port in blocking mode with explicit connect timeout
            socketChannel.socket().connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            socketChannel.finishConnect();

            // Set non blocking and register the read event
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);

            connected = true;

            Thread subThread = new Thread(this);
            subThread.setName("Receive Thread");
            subThread.start();

            Thread keepThread = new Thread(new KeepAliveTask());
            keepThread.setName("Heartbeat Thread");
            keepThread.start();

            clientConnected();

            return true;
        } catch (Exception ignore) {
            connected = false;

            closeSocket();
            reconnect();
        }

        return false;
    }

    private void addToQueue(JSONObject jsonObject) {

        // Only queue up to 100 messages
        if (sendQueue.size() < 100) {
            sendQueue.offer(jsonObject.toString());
        }
    }

    private void closeSocket() {

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reconnect() {

        if (!reconnecting.get()) {
            reconnecting.set(true);

            new Thread(new ReconnectTask()).start();
        }
    }

    public void close(boolean force) {

        if (connected) {
            connected = false;

            // Close selector and the socket channel
            closeSocket();

            if (!force) {
                reconnect();
            }
        }
    }

    public void write(JSONObject jsonObject, boolean queueEnabled) {

        if (socketChannel == null || !socketChannel.isConnected()) {
            if (queueEnabled) {
                addToQueue(jsonObject);
            }
            return;
        }

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
            if (queueEnabled) {
                addToQueue(jsonObject);
            }

            close(false);
        }
    }

    public void write(JSONObject jsonObject) {

        write(jsonObject, true);
    }

    @Override
    public void run() {

        while (connected) {
            try {
                if (selector.select() == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isReadable()) {
                        int read = socketChannel.read(byteBuffer);

                        if (read == -1) {
                            close(false);
                            return;
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

                            received(new JSONObject(new String(bytes, "UTF-8")));
                        }

                        byteBuffer.compact();
                    }
                }
            } catch (Exception ignore) {
                close(false);
            }
        }

        close(true);
    }

    public List<ClusterServer> clusterServers() {

        return clusterServers;
    }

    public boolean isConnected() {

        return connected;
    }

    private class KeepAliveTask implements Runnable {

        @Override
        public void run() {

            // Construct the keep alive message
            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_KEEP_ALIVE.getCode())
                    .put("ch", "keep-alive");

            while (connected) {
                // Try to send a keep alive to detect connection lost
                write(jsonObject);

                try {
                    Thread.sleep(KEEP_ALIVE_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ReconnectTask implements Runnable {

        @Override
        public void run() {

            ClusterServer connectTo = clusterServers.get(clusterServerIndex);

            while (!connect(connectTo.host(), connectTo.port())) {

                if (reconnectPause > 0) {
                    try {
                        Thread.sleep(reconnectPause);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                clusterServerIndex++;
                if (reconnectPause < 1000) {
                    reconnectPause += 50;
                }

                if (clusterServers.size() == clusterServerIndex) {
                    clusterServerIndex = 0;
                }

                // Assign the new cluster server
                connectTo = clusterServers.get(clusterServerIndex);
            }

            reconnecting.set(false);

            if (!sendQueue.isEmpty()) {

                try {
                    // Give the subscribers a chance to connect and register their channels first
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                sendQueue.forEach(s -> write(new JSONObject(s), false));

                // Clear the queue to avoid duplicated messages
                sendQueue.clear();
            }

            clientReconnected();
        }
    }
}
