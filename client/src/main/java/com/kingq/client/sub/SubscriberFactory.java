package com.kingq.client.sub;

import com.kingq.client.sub.impl.SubscriberImpl;
import com.kingq.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by spring on 25.03.2017.
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

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

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

        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host cannot be null or empty");
        }

        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        if (subscriberName == null || subscriberName.isEmpty()) {
            throw new IllegalArgumentException("subscriberName cannot be null or empty");
        }

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

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        if (subscriberName == null || subscriberName.isEmpty()) {
            throw new IllegalArgumentException("subscriberName cannot be null or empty");
        }

        return new SubscriberImpl(clusterServers, subscriberName);
    }

    /**
     * Creates a new subscriber instance which connects to the first cluster server.
     *
     * @param clusterServers The list of cluster servers.
     * @return A new instance of a subscriber implementation.
     */
    public static Subscriber create(List<ClusterServer> clusterServers) {

        if (clusterServers == null || clusterServers.isEmpty()) {
            throw new IllegalArgumentException("clusterServers cannot be null or empty");
        }

        return new SubscriberImpl(clusterServers);
    }
}
