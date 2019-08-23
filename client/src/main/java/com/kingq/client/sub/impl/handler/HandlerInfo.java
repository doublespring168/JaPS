package com.kingq.client.sub.impl.handler;

import java.lang.reflect.ParameterizedType;

/**
 * Created by spring on 25.03.2017.
 */
public class HandlerInfo<T> {

    private ChannelHandler<T> messageHandler;

    private Class<?> clazz;

    private ClassType classType;

    public HandlerInfo(ChannelHandler<T> messageHandler) {

        this.messageHandler = messageHandler;
        //noinspection unchecked
        this.clazz = (Class<T>) ((ParameterizedType) messageHandler.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.classType = (this.clazz.getSimpleName().equals("JSONObject")) ? ClassType.JSON : ClassType.GSON;
    }

    public ChannelHandler<T> messageHandler() {

        return messageHandler;
    }

    public Class<?> clazz() {

        return clazz;
    }

    public ClassType classType() {

        return classType;
    }
}
