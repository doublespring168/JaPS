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
import de.jackwhite20.japs.client.sub.impl.exception.SubscriberConnectException;
import de.jackwhite20.japs.client.sub.impl.handler.*;
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

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class SubscriberImpl implements Subscriber, Runnable {

    private static final int INITIAL_STRING_BUILDER_SIZE = 256;

    private boolean connected;

    private String host;

    private int port;

    private SocketChannel socketChannel;

    private Selector selector;

    private ByteBuffer byteBuffer = ByteBuffer.allocate(4096);

    private static HashMap<String, HandlerInfo> handlers = new HashMap<>();

    private static HashMap<String, MultiHandlerInfo> multiHandlers = new HashMap<>();

    private CountDownLatch connectLatch = new CountDownLatch(1);

    private StringBuilder stringBuilder = new StringBuilder(INITIAL_STRING_BUILDER_SIZE);

    private Gson gson = new Gson();

    public SubscriberImpl(String host, int port) {

        this.host = host;
        this.port = port;

        connect();
    }

    private void connect() {

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
        } catch (Exception e) {
            throw new SubscriberConnectException("cant connect to " + host + ":" + port);
        }
    }

    private void countDown() {

        connectLatch.countDown();
    }

    private void write(String data) {

        try {
            socketChannel.write(ByteBuffer.wrap((data + "\n").getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {

        if(connected) {
            connected = false;

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
    }

    @Override
    public void subscribe(String channel, Class<? extends ChannelHandler> handler) {

        try {
            //noinspection unchecked
            handlers.put(channel, new HandlerInfo(handler.newInstance()));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", 0);
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
            for (Method method : object.getClass().getMethods()) {
                if(method.getParameterCount() == 1 && method.getParameterTypes()[0].getSimpleName().equals("JSONObject")) {
                    if (method.isAnnotationPresent(Key.class) && method.isAnnotationPresent(Value.class)) {
                        entries.add(new MultiHandlerInfo.Entry(method.getAnnotation(Key.class), method.getAnnotation(Value.class), method));
                    }
                }
            }

            String channel = handler.getAnnotation(Channel.class).value();

            if(channel.isEmpty()) {
                throw new IllegalStateException("channel is empty");
            }

            multiHandlers.put(channel, new MultiHandlerInfo(entries, object));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("op", 0);
            jsonObject.put("ch", channel);

            write(jsonObject.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscribe(String channel) {

        handlers.remove(channel);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("op", 1);
        jsonObject.put("ch", channel);

        write(jsonObject.toString());
    }

    @Override
    public boolean connected() {

        return connected;
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

                    if (!key.isValid())
                        continue;

                    if(key.isConnectable()) {
                        socketChannel.finishConnect();

                        socketChannel.register(selector, SelectionKey.OP_READ);

                        countDown();
                    }

                    if(key.isReadable()) {
                        int read = socketChannel.read(byteBuffer);

                        if(read == -1) {
                            disconnect();
                            return;
                        }

                        byteBuffer.flip();

                        byte[] bytes = new byte[read];
                        byteBuffer.get(bytes);

                        for (byte aByte : bytes) {
                            char c = ((char) aByte);
                            stringBuilder.append(c);

                            if (c == '\n') {
                                String jsonLine = stringBuilder.toString();
                                stringBuilder.setLength(0);

                                JSONObject jsonObject = new JSONObject(jsonLine);

                                String channel = jsonObject.getString("ch");

                                jsonObject.remove("ch");

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

                                                    // Invoke the matching method
                                                    entry.method().invoke(multiHandlerInfo.object(), jsonObject);
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
            } catch (ConnectException ce) {
                throw new SubscriberConnectException("can't connect to " + host + ":" + port);
            } catch (Exception e) {
                break;
            }
        }

        countDown();

        disconnect();
    }
}
