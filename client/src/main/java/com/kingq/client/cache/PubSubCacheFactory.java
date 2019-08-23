package com.kingq.client.cache;

import com.kingq.client.cache.impl.PubSubCacheImpl;
import com.kingq.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by spring on 13.06.2017.
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
