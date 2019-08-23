package com.kingq.client.sub;

import com.kingq.client.sub.impl.handler.ChannelHandler;
import com.kingq.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by spring on 25.03.2017.
 */
public interface Subscriber {

    /**
     * Disconnects the subscriber.
     * If you are not forcing a disconnect, this method will block until the reconnect is successful.
     *
     * @param force If true, the subscriber will not try to reconnect
     */
    void disconnect(boolean force);

    /**
     * Disconnects the subscriber without trying to reconnect.
     * <p>
     * Same as invoking disconnect(true).
     */
    void disconnect();

    /**
     * Checks if the given channel is subscribed.
     *
     * @param channel The channel to check.
     * @return True if it is subscribed, otherwise false.
     */
    boolean hasSubscription(String channel);

    /**
     * Subscribes a channel and sets the handler for it.
     * <p>
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param channel The channel to subscribe to.
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(String channel, Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     * <p>
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the multi handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     * <p>
     * If the channel is already subscribed, the multi channel handler will be overwritten.
     *
     * @param handler The handler which is responsible for the messages received in that channel and the key value method matches.
     */
    void subscribeMulti(Class<?> handler);

    /**
     * Unsubscribe a channel.
     *
     * @param channel The channel to unsubscribe.
     */
    void unsubscribe(String channel);

    /**
     * Returns if the subscriber is connected.
     *
     * @return True if connected, otherwise false.
     */
    boolean connected();

    /**
     * Returns the name of the subscriber.
     *
     * @return The name.
     */
    String name();

    /**
     * Returns an unmodifiable list of the cluster servers.
     *
     * @return The unmodifiable list of the cluster servers.
     */
    List<ClusterServer> clusterServers();
}
