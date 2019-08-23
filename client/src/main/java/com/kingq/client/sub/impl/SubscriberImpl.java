package com.kingq.client.sub.impl;

import com.google.gson.Gson;
import com.kingq.client.sub.Subscriber;
import com.kingq.client.sub.impl.handler.ChannelHandler;
import com.kingq.client.sub.impl.handler.ClassType;
import com.kingq.client.sub.impl.handler.HandlerInfo;
import com.kingq.client.sub.impl.handler.MultiHandlerInfo;
import com.kingq.client.sub.impl.handler.annotation.Channel;
import com.kingq.client.sub.impl.handler.annotation.Key;
import com.kingq.client.sub.impl.handler.annotation.Value;
import com.kingq.client.util.NameGeneratorUtil;
import com.kingq.shared.config.ClusterServer;
import com.kingq.shared.net.OpCode;
import com.kingq.shared.nio.NioSocketClient;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by spring on 25.03.2017.
 */
public class SubscriberImpl extends NioSocketClient implements Subscriber {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(0);

    private Map<String, HandlerInfo> handlers = new HashMap<>();

    private Map<String, MultiHandlerInfo> multiHandlers = new HashMap<>();

    private Gson gson = new Gson();

    public SubscriberImpl(String host, int port) {

        this(host, port, NameGeneratorUtil.generateName("subscriber", ID_COUNTER.getAndIncrement()));
    }

    public SubscriberImpl(String host, int port, String name) {

        this(Collections.singletonList(new ClusterServer(host, port)), name);
    }

    public SubscriberImpl(List<ClusterServer> clusterServers) {

        this(clusterServers, NameGeneratorUtil.generateName("subscriber", ID_COUNTER.getAndIncrement()));
    }

    public SubscriberImpl(List<ClusterServer> clusterServers, String name) {

        super(clusterServers, name);
    }

    @Override
    public void clientReconnected() {

        if (handlers != null) {
            // Resubscribe the normal handlers
            for (Map.Entry<String, HandlerInfo> handlerInfoEntry : handlers.entrySet()) {
                subscribe(handlerInfoEntry.getValue().messageHandler().getClass());
            }
        }

        if (multiHandlers != null) {
            // Resubscribe the multi handlers
            for (Map.Entry<String, MultiHandlerInfo> handlerInfoEntry : multiHandlers.entrySet()) {
                subscribeMulti(handlerInfoEntry.getValue().object().getClass());
            }
        }
    }

    private String getChannelFromAnnotation(Class<?> clazz) {

        if (!clazz.isAnnotationPresent(Channel.class)) {
            throw new IllegalArgumentException("the handler class " + clazz.getSimpleName() + " has no 'Channel' annotation");
        }

        String channel = clazz.getAnnotation(Channel.class).value();

        if (channel.isEmpty()) {
            throw new IllegalStateException("value of the 'Channel' annotation of class " + clazz.getSimpleName() + " is empty");
        }

        return channel;
    }

    @Override
    public void clientConnected() {

        // Register with our name
        write(new JSONObject()
                .put("op", OpCode.OP_SUBSCRIBER_SET_NAME.getCode())
                .put("su", name));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(JSONObject jsonObject) {

        String channel = ((String) jsonObject.remove("ch"));

        if (channel == null || channel.isEmpty()) {
            return;
        }

        HandlerInfo handlerInfo = handlers.get(channel);

        if (handlerInfo != null) {
            if (handlerInfo.classType() == ClassType.JSON) {
                handlerInfo.messageHandler().onMessage(channel, jsonObject);
            } else {
                handlerInfo.messageHandler().onMessage(channel, gson.fromJson(jsonObject.toString(), handlerInfo.clazz()));
            }
        } else {
            MultiHandlerInfo multiHandlerInfo = multiHandlers.get(channel);

            if (multiHandlerInfo != null) {
                //noinspection Convert2streamapi
                for (MultiHandlerInfo.Entry entry : multiHandlerInfo.entries()) {
                    if (!jsonObject.isNull(entry.key().value())) {
                        if (jsonObject.get(entry.key().value()).equals(entry.value().value())) {
                            // Remove matched key value pair
                            jsonObject.remove(entry.key().value());

                            if (entry.classType() == ClassType.JSON) {
                                try {
                                    // Invoke the matching method
                                    entry.method().invoke(multiHandlerInfo.object(), jsonObject);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    // Deserialize with gson
                                    entry.method().invoke(multiHandlerInfo.object(), gson.fromJson(jsonObject.toString(), entry.paramClass()));
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
    }    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
    }



    @Override
    public void disconnect(boolean force) {

        close(force);
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public boolean hasSubscription(String channel) {

        return handlers.containsKey(channel) || multiHandlers.containsKey(channel);
    }

    @Deprecated
    @Override
    public void subscribe(String channel, Class<? extends ChannelHandler> handler) {

        try {
            //noinspection unchecked
            handlers.put(channel, new HandlerInfo(handler.newInstance()));

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_REGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void subscribe(Class<? extends ChannelHandler> handler) {

        // Get channel and check the class for annotation etc.
        String channel = getChannelFromAnnotation(handler);

        //noinspection deprecation
        subscribe(channel, handler);
    }

    @Override
    public void subscribeMulti(Class<?> handler) {

        // Get channel and check the class for annotation etc.
        String channel = getChannelFromAnnotation(handler);

        try {
            List<MultiHandlerInfo.Entry> entries = new ArrayList<>();

            Object object = handler.newInstance();
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 1) {
                    if (method.isAnnotationPresent(Key.class) && method.isAnnotationPresent(Value.class)) {
                        entries.add(new MultiHandlerInfo.Entry(method.getAnnotation(Key.class), method.getAnnotation(Value.class), method.getParameterTypes()[0], (method.getParameterTypes()[0].getSimpleName().equals("JSONObject")) ? ClassType.JSON : ClassType.GSON, method));
                    }
                }
            }

            multiHandlers.put(channel, new MultiHandlerInfo(entries, object));

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_REGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unsubscribe(String channel) {

        // Only send unsubscribe if the channel is subscribed
        if (handlers.containsKey(channel) || multiHandlers.containsKey(channel)) {
            handlers.remove(channel);
            multiHandlers.remove(channel);

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_UNREGISTER_CHANNEL.getCode())
                    .put("ch", channel);

            write(jsonObject);
        }
    }

    @Override
    public boolean connected() {

        return isConnected();
    }

    @Override
    public String name() {

        return name;
    }


}
