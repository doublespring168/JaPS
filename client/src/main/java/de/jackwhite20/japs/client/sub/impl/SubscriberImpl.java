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

package de.jackwhite20.japs.client.sub.impl;

import com.google.gson.Gson;
import de.jackwhite20.japs.client.sub.Subscriber;
import de.jackwhite20.japs.client.sub.impl.handler.*;
import de.jackwhite20.japs.client.sub.impl.handler.annotation.Channel;
import de.jackwhite20.japs.client.sub.impl.handler.annotation.Key;
import de.jackwhite20.japs.client.sub.impl.handler.annotation.Value;
import de.jackwhite20.japs.client.util.ClusterServer;
import de.jackwhite20.japs.client.util.NameGeneratorUtil;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class SubscriberImpl implements Subscriber, Runnable {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private static final int BUFFER_SIZE = 4096;

    private static final int OP_SUBSCRIBE = 0;

    private static final int OP_UNSUBSCRIBE = 1;

    private static final int OP_NAME = 3;

    private boolean connected;

    private String name;

    private List<ClusterServer> clusterServers;

    private int clusterServerIndex = 0;

    private SocketChannel socketChannel;

    private Selector selector;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private static Map<String, HandlerInfo> handlers = new HashMap<>();

    private static Map<String, MultiHandlerInfo> multiHandlers = new HashMap<>();

    private CountDownLatch connectLatch = new CountDownLatch(1);

    private long reconnectPause = 0;

    private Gson gson = new Gson();

    public SubscriberImpl(String host, int port) {

        this(host, port, NameGeneratorUtil.generateName("subscriber", ID_COUNTER.getAndIncrement()));
    }

    public SubscriberImpl(String host, int port, String name) {

        this(Collections.singletonList(new ClusterServer(host, port)), name);
    }

    public SubscriberImpl(List<ClusterServer> clusterServers, String name) {

        this.clusterServers = clusterServers;
        this.name = name;

        // Get the first cluster server info
        String firstHost = clusterServers.get(clusterServerIndex).host();
        int firstPort = clusterServers.get(clusterServerIndex).port();

        // Try to connect
        connect(firstHost, firstPort);
    }

    private void connect(String host, int port) {

        try {
            selector = Selector.open();

            // Open a new socket channel and connect to the host and port
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            // Disable the Nagle algorithm
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            new Thread(this).start();

            connectLatch.await();
        } catch (Exception ignore) {
            closeSocket();
            reconnect();
        }
    }

    private void closeSocket() {

        if(selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void reconnect() {

        if(reconnectPause > 0) {
            try {
                Thread.sleep(reconnectPause);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        clusterServerIndex++;
        if(reconnectPause < 1000) {
            reconnectPause += 100;
        }

        if(clusterServers.size() == clusterServerIndex) {
            clusterServerIndex = 0;
        }

        // Reset the latch
        connectLatch = new CountDownLatch(1);

        // Reconnect to the new cluster server
        connect(clusterServers.get(clusterServerIndex).host(), clusterServers.get(clusterServerIndex).port());

        // Resend the subscribed channels
        for (Map.Entry<String, HandlerInfo> handlerInfoEntry : handlers.entrySet()) {
            subscribe(handlerInfoEntry.getKey(), handlerInfoEntry.getValue().messageHandler().getClass());
        }

        for (Map.Entry<String, MultiHandlerInfo> handlerInfoEntry : multiHandlers.entrySet()) {
            subscribe(handlerInfoEntry.getValue().object().getClass());
        }
    }

    private void countDown() {

        connectLatch.countDown();
    }

    private void write(String data) {

        try {
            // Send the json data and prepend the length
            byte[] bytes = data.getBytes("UTF-8");
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
            byteBuffer.putInt(bytes.length);
            byteBuffer.put(bytes);

            // Prepare the buffer for writing
            byteBuffer.flip();

            socketChannel.write(byteBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect(boolean force) {

        if(connected) {
            connected = false;

            // Close selector and the socket channel
            closeSocket();

            if(!force) {
                reconnect();
            }
        }
    }

    @Override
    public boolean hasSubscription(String channel) {

        return handlers.containsKey(channel) || multiHandlers.containsKey(channel);
    }

    @Override
    public void subscribe(String channel, Class<? extends ChannelHandler> handler) {

        try {
            //noinspection unchecked
            handlers.put(channel, new HandlerInfo(handler.newInstance()));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", OP_SUBSCRIBE);
            jsonObject.put("ch", channel);

            write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void subscribe(Class<?> handler) {

        if(!handler.isAnnotationPresent(Channel.class)) {
            throw new IllegalArgumentException("the handler class has no 'Channel' annotation");
        }

        try {
            List<MultiHandlerInfo.Entry> entries = new ArrayList<>();

            Object object = handler.newInstance();
            for (Method method : object.getClass().getDeclaredMethods()) {
                if(method.getParameterCount() == 1) {
                    if (method.isAnnotationPresent(Key.class) && method.isAnnotationPresent(Value.class)) {
                        entries.add(new MultiHandlerInfo.Entry(method.getAnnotation(Key.class), method.getAnnotation(Value.class), method.getParameterTypes()[0], (method.getParameterTypes()[0].getSimpleName().equals("JSONObject")) ? ClassType.JSON : ClassType.GSON, method));
                    }
                }
            }

            String channel = handler.getAnnotation(Channel.class).value();

            if(channel.isEmpty()) {
                throw new IllegalStateException("value of the 'Channel' annotation of class " + handler.getSimpleName() + " is empty");
            }

            multiHandlers.put(channel, new MultiHandlerInfo(entries, object));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", OP_SUBSCRIBE);
            jsonObject.put("ch", channel);

            write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscribe(String channel) {

        // Only send unsubscribe if the channel is subscribed
        if(handlers.containsKey(channel) || multiHandlers.containsKey(channel)) {
            handlers.remove(channel);
            multiHandlers.remove(channel);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", OP_UNSUBSCRIBE);
            jsonObject.put("ch", channel);

            write(jsonObject.toString());
        }
    }

    @Override
    public boolean connected() {

        return connected;
    }

    @Override
    public String name() {

        return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {

        connected = true;

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

                    if(key.isConnectable()) {
                        socketChannel.finishConnect();

                        socketChannel.register(selector, SelectionKey.OP_READ);

                        // Register with our name
                        write(new JSONObject().put("op", OP_NAME).put("su", name).toString());

                        countDown();
                    }

                    if(key.isReadable()) {
                        int read = socketChannel.read(byteBuffer);

                        if(read == -1) {
                            disconnect(false);
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

                            String json = new String(bytes, "UTF-8");

                            JSONObject jsonObject = new JSONObject(json);

                            String channel = ((String) jsonObject.remove("ch"));

                            if(channel == null || channel.isEmpty()) {
                                continue;
                            }

                            HandlerInfo handlerInfo = handlers.get(channel);

                            if(handlerInfo != null) {
                                if (handlerInfo.classType() == ClassType.JSON) {
                                    handlerInfo.messageHandler().onMessage(channel, jsonObject);
                                } else {
                                    handlerInfo.messageHandler().onMessage(channel, gson.fromJson(jsonObject.toString(), handlerInfo.clazz()));
                                }
                            }else {
                                MultiHandlerInfo multiHandlerInfo = multiHandlers.get(channel);

                                if(multiHandlerInfo != null) {
                                    for (MultiHandlerInfo.Entry entry : multiHandlerInfo.entries()) {
                                        if (!jsonObject.isNull(entry.key().value())) {
                                            if (jsonObject.get(entry.key().value()).equals(entry.value().value())) {
                                                // Remove matched key value pair
                                                jsonObject.remove(entry.key().value());

                                                if(entry.classType() == ClassType.JSON) {
                                                    // Invoke the matching method
                                                    entry.method().invoke(multiHandlerInfo.object(), jsonObject);
                                                } else {
                                                    // Deserialize with gson
                                                    entry.method().invoke(multiHandlerInfo.object(), gson.fromJson(jsonObject.toString(), entry.paramClass()));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        byteBuffer.compact();
                    }
                }
            } catch (ConnectException ignore) {
                disconnect(false);
            } catch (Exception ignore) {
                break;
            }
        }

        countDown();

        disconnect(true);
    }
}
