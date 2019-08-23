package com.kingq.server.config;

import com.kingq.shared.config.ClusterServer;

import java.util.List;

/**
 * Created by spring on 25.03.2017.
 */
public class Config {

    private String host;

    private int port;

    private int backlog = 50;

    private boolean debug = true;

    private int workerThreads = Runtime.getRuntime().availableProcessors();

    private List<ClusterServer> cluster;

    private int cleanupInterval = -1;

    private int snapshotInterval = -1;

    public Config(String host, int port, int backlog, boolean debug, int workerThreads, List<ClusterServer> cluster, int cleanupInterval, int snapshotInterval) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.debug = debug;
        this.workerThreads = workerThreads;
        this.cluster = cluster;
        this.cleanupInterval = cleanupInterval;
        this.snapshotInterval = snapshotInterval;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    public int backlog() {

        return backlog;
    }

    public boolean debug() {

        return debug;
    }

    public int workerThreads() {

        return workerThreads;
    }

    public List<ClusterServer> cluster() {

        return cluster;
    }

    public int cleanupInterval() {

        return cleanupInterval;
    }

    public int snapshotInterval() {

        return snapshotInterval;
    }

    @Override
    public String toString() {

        return "Config{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", backlog=" + backlog +
                ", debug=" + debug +
                ", workerThreads=" + workerThreads +
                ", cluster=" + cluster +
                ", cleanupInterval=" + cleanupInterval +
                ", snapshotInterval=" + snapshotInterval +
                '}';
    }
}
