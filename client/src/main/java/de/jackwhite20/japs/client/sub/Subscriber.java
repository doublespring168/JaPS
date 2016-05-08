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

package de.jackwhite20.japs.client.sub;

import de.jackwhite20.japs.client.sub.impl.handler.ChannelHandler;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public interface Subscriber {

    /**
     * Disconnects the publisher
     *
     * @param force If true, the subscriber will not try to reconnect
     */
    void disconnect(boolean force);

    /**
     * Checks if the given channel is subscribed.
     *
     * @param channel The channel to check.
     * @return True if it is subscribed, otherwise false.
     */
    boolean hasSubscription(String channel);

    /**
     * Subscribes a channel and sets the handler for it.
     *
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param channel The channel to subscribe to.
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(String channel, Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     *
     * If the channel is already subscribed, the channel handler will be overwritten.
     *
     * @param handler The handler which is responsible for the messages received in that channel.
     */
    void subscribe(Class<? extends ChannelHandler> handler);

    /**
     * Subscribes a channel and sets the multi handler for it.
     * The class must have a Channel annotation with the channel the class is responsible for.
     *
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
}
