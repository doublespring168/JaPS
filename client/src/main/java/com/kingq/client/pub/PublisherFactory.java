package com.kingq.client.pub;

import com.kingq.client.pub.impl.PublisherImpl;
import com.kingq.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by spring on 25.03.2017.
 */
public final class PublisherFactory {

    private PublisherFactory() {
        // no instance
    }

    /**
     * Creates a new publisher instance which connects to the given host and port.
     *
     * @param host The host to connect to.
     * @param port The port to connect to.
     * @return A new instance of a publisher implementation.
     */
    public static Publisher create(String host, int port) {

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        return new PublisherImpl(host, port);
    }

    /**
     * Creates a new publisher instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a publisher implementation.
     */
    public static Publisher create(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        return new PublisherImpl(clusterServers);
    }
}
