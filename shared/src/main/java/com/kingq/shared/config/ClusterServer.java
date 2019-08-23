package com.kingq.shared.config;

public class ClusterServer {

    private String host;

    private int port;

    public ClusterServer(String host, int port) {

        this.host = host;
        this.port = port;
    }

    public String host() {

        return host;
    }

    public int port() {

        return port;
    }

    @Override
    public String toString() {

        return "ClusterServer{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}