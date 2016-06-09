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

import de.jackwhite20.japs.client.sub.impl.SubscriberImpl;
import de.jackwhite20.japs.client.util.ClusterServer;

import java.util.List;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public final class SubscriberFactory {

    private SubscriberFactory() {
        // no instance
    }

    /**
     * Creates a new subscriber instance which connects to the given host and port.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(String host, int port) {

        return new SubscriberImpl(host, port);
    }

    /**
     * Creates a new subscriber instance which connects to the given host and port.
     *
     * @param host           The host to connect to.
     * @param port           The port to connect to.
     * @param subscriberName The subscriber name.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(String host, int port, String subscriberName) {

        return new SubscriberImpl(host, port, subscriberName);
    }

    /**
     * Creates a new subscriber instance with the given name which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @param subscriberName The subscriber name.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(List<ClusterServer> clusterServers, String subscriberName) {

        return new SubscriberImpl(clusterServers, subscriberName);
    }

    /**
     * Creates a new subscriber instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(List<ClusterServer> clusterServers) {

        return new SubscriberImpl(clusterServers);
    }
}
