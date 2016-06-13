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
import de.jackwhite20.japs.client.cache.PubSubCache;
import de.jackwhite20.japs.client.cache.impl.PubSubCacheImpl;
import de.jackwhite20.japs.client.pub.AsyncPublisher;
import de.jackwhite20.japs.client.pub.Publisher;
import de.jackwhite20.japs.client.util.ClusterServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class PublisherImpl implements Publisher {

    private static final long KEEP_ALIVE_TIME = 1000;

    private boolean connected;

    private List<ClusterServer> clusterServers;

    private int clusterServerIndex = 0;

    private SocketChannel socketChannel;

    private long reconnectPause = 0;

    private Gson gson = new Gson();

    private ExecutorService executorService;

    private AsyncPublisher asyncPublisher;

    private AtomicBoolean reconnecting = new AtomicBoolean(false);

    private ConcurrentLinkedQueue<JSONObject> sendQueue = new ConcurrentLinkedQueue<>();

    private PubSubCache pubSubCache;

    public PublisherImpl(String host, int port) {

        this(Collections.singletonList(new ClusterServer(host, port)));
    }

    public PublisherImpl(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        this.clusterServers = clusterServers;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("Publisher Thread");

            return thread;
        });
        this.asyncPublisher = new AsyncPublisherImpl(executorService, this);

        // Get the first cluster server info
        String firstHost = clusterServers.get(clusterServerIndex).host();
        int firstPort = clusterServers.get(clusterServerIndex).port();

        // Try to connect
        connect(firstHost, firstPort);
    }

    private boolean connect(String host, int port) {

        try {
            // Open a new socket channel and connect to the host and port
            socketChannel = SocketChannel.open();
            // Disable the Nagle algorithm
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.connect(new InetSocketAddress(host, port));

            this.pubSubCache = new PubSubCacheImpl(socketChannel);

            // We are connected successfully
            connected = true;
            reconnectPause = 0;

            new Thread(new KeepAliveTask()).start();

            return true;
        } catch (IOException e) {
            closeSocket();
            reconnect();
        }

        return false;
    }

    private void reconnect() {

        if (!reconnecting.get()) {
            reconnecting.set(true);

            new Thread(new ConnectTask()).start();
        }
    }

    private void closeSocket() {

        // If not null disconnect the socket channel
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addToQueue(String channel, JSONObject jsonObject) {

        if (sendQueue.size() < 100) {
            sendQueue.offer(jsonObject.put("ch", channel));
        }
    }

    @Override
    public void disconnect(boolean force) {

        if (connected) {
            // We are not connected anymore
            connected = false;

            // Close the socket channel
            closeSocket();

            if (!force) {
                // Try to reconnect
                reconnect();
            } else {
                // Shutdown our executor service
                executorService.shutdown();
            }
        }
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public void publish(String channel, JSONObject jsonObject) {

        publish(channel, null, jsonObject);
    }

    @Override
    public void publishAll(String channel, JSONObject... jsonObjects) {

        for (JSONObject jsonObject : jsonObjects) {
            publish(channel, jsonObject);
        }
    }

    @Override
    public void publish(String channel, String subscriberName, JSONObject jsonObject) {

        if (channel == null || channel.isEmpty()) {
            throw new IllegalArgumentException("channel cannot be null or empty");
        }

        if (jsonObject == null || jsonObject.length() == 0) {
            throw new IllegalArgumentException("jsonObject cannot be null or empty");
        }

        if (socketChannel == null || !socketChannel.isConnected()) {
            addToQueue(channel, jsonObject);
            return;
        }

        // Set the op code, channel
        jsonObject.put("op", 2);
        jsonObject.put("ch", channel);
        // Set the subscriber name if it is not null
        if (subscriberName != null) {
            jsonObject.put("su", subscriberName);
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
            if (!channel.equals("keep-alive")) {
                addToQueue(channel, jsonObject);
            }

            disconnect(false);
        }
    }

    @Override
    public void publishAll(String channel, String subscriberName, JSONObject... jsonObjects) {

        for (JSONObject jsonObject : jsonObjects) {
            publish(channel, subscriberName, jsonObject);
        }
    }

    @Override
    public void publish(String channel, String json) {

        publish(channel, null, json);
    }

    @Override
    public void publish(String channel, String subscriberName, String json) {

        if (json == null || json.isEmpty()) {
            throw new IllegalArgumentException("json cannot be null or empty");
        }

        try {
            // Publish it as JSONObject that we can add the op code and channel
            publish(channel, subscriberName, new JSONObject(json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publish(String channel, Object object) {

        publish(channel, null, object);
    }

    @Override
    public void publishAll(String channel, Object... objects) {

        for (Object object : objects) {
            publish(channel, null, object);
        }
    }

    @Override
    public void publish(String channel, String subscriberName, Object object) {

        if (object == null) {
            throw new IllegalArgumentException("object cannot be null");
        }

        // Publish the serialized object as json string
        publish(channel, subscriberName, gson.toJson(object));
    }

    @Override
    public void publishAll(String channel, String subscriberName, Object... objects) {

        for (Object object : objects) {
            publish(channel, subscriberName, object);
        }
    }

    @Override
    public boolean connected() {

        return connected;
    }

    @Override
    public AsyncPublisher async() {

        return asyncPublisher;
    }

    @Override
    public PubSubCache cache() {

        return pubSubCache;
    }

    private class KeepAliveTask implements Runnable {

        @Override
        public void run() {

            while (connected) {
                // Try to send a keep alive to detect connection lost
                publish("keep-alive", new JSONObject().put("time", System.currentTimeMillis()));

                try {
                    Thread.sleep(KEEP_ALIVE_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ConnectTask implements Runnable {

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
                    reconnectPause += 100;
                }

                if (clusterServers.size() == clusterServerIndex) {
                    clusterServerIndex = 0;
                }

                // Assign the new cluster server
                connectTo = clusterServers.get(clusterServerIndex);
            }

            // We are no longer reconnecting
            reconnecting.set(false);

            if (!sendQueue.isEmpty()) {

                try {
                    // Give the subscriber a chance to connect and register his channels first
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (JSONObject jsonObject : sendQueue) {
                    publish(jsonObject.getString("ch"), jsonObject);
                }
            }
        }
    }
}
