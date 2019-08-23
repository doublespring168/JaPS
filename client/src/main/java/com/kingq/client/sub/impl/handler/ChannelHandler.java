package com.kingq.client.sub.impl.handler;

/**
 * Created by spring on 25.03.2017.
 */
public abstract class ChannelHandler<T> {

    public abstract void onMessage(String channel, T message);
}

