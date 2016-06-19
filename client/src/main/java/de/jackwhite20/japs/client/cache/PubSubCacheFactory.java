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

package de.jackwhite20.japs.client.cache;

import de.jackwhite20.japs.client.cache.impl.PubSubCacheImpl;
import de.jackwhite20.japs.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by JackWhite20 on 13.06.2016.
 */
public final class PubSubCacheFactory {

    private PubSubCacheFactory() {
        // no instance
    }

    /**
     * Creates a new pub sub cache implementation and connects to the given host and port.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new instance of a pub sub cache implementation.
     */
    public static PubSubCache create(String host, int port) {

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        return new PubSubCacheImpl(host, port);
    }

    /**
     * Creates a new pub sub cache instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a pub sub cache implementation.
     */
    public static PubSubCache create(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        return new PubSubCacheImpl(clusterServers);
    }
}
