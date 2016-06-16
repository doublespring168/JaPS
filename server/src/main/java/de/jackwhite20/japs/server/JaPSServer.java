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

package de.jackwhite20.japs.server;

import de.jackwhite20.japs.client.OpCode;
import de.jackwhite20.japs.server.cache.JaPSCache;
import de.jackwhite20.japs.server.config.Config;
import de.jackwhite20.japs.server.network.Connection;
import de.jackwhite20.japs.server.network.SelectorThread;
import de.jackwhite20.japs.server.util.RoundRobinList;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class JaPSServer implements Runnable {

    private static final Logger LOGGER = JaPS.getLogger();

    private String host;

    private int port;

    private int backlog;

    private Selector selector;

    private ServerSocketChannel serverSocketChannel;

    private Map<String, List<Connection>> channelSessions = new ConcurrentHashMap<>();

    private ReentrantLock selectorLock = new ReentrantLock();

    private List<SelectorThread> selectorThreads = new ArrayList<>();

    private RoundRobinList<SelectorThread> selectorRoundRobin;

    private ExecutorService workerPool;

    private int workerThreads;

    private List<ClusterPublisher> clusterPublisher = new ArrayList<>();

    private JaPSCache cache;

    public JaPSServer(String host, int port, int backlog, boolean debug, int workerThreads, List<Config.ClusterServer> cluster, int cleanupInterval) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.workerThreads = workerThreads;
        this.cache = new JaPSCache(cleanupInterval);

        LOGGER.setLevel((debug) ? Level.FINE : Level.INFO);

        start();

        // Check if there are cluster servers to avoid unnecessary logic execution
        if (cluster.size() > 0) {
            new Thread(() -> {
                while (cluster.size() > 0) {
                    LOGGER.info("Trying to connecting to all cluster servers");

                    Iterator<Config.ClusterServer> clusterServerIterator = cluster.iterator();
                    while (clusterServerIterator.hasNext()) {
                        Config.ClusterServer clusterServer = clusterServerIterator.next();

                        // Remove the own endpoint of this instance (does not work if it is bound to 0.0.0.0)
                        if (clusterServer.port() == port && clusterServer.host().equals(host)) {
                            clusterServerIterator.remove();
                            continue;
                        }

                        try {
                            //Publisher publisher = PublisherFactory.create(clusterServer.host(), clusterServer.port());
                            ClusterPublisher cb = new ClusterPublisher(clusterServer.host(), clusterServer.port());

                            if (cb.connect()) {
                                clusterPublisher.add(cb);

                                clusterServerIterator.remove();

                                cb.write(new JSONObject()
                                        .put("op", OpCode.OP_CLUSTER_INFO_SET.getCode())
                                        .put("host", host)
                                        .put("port", port));

                                LOGGER.log(Level.INFO, "Connected to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                            } else {
                                LOGGER.log(Level.SEVERE, "Could not connect to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Could not connect to cluster server {0}:{1}", new Object[]{clusterServer.host(), String.valueOf(clusterServer.port())});
                        }
                    }

                    if (cluster.size() == 0) {
                        break;
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LOGGER.info("Cluster servers are connected successfully!");
            }).start();
        }
    }

    public JaPSServer(Config config) {

        this(config.host(), config.port(), config.backlog(), config.debug(), config.workerThreads(), config.cluster(), config.cleanupInterval());
    }

    private void start() {

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(host, port), backlog);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            workerPool = Executors.newFixedThreadPool(workerThreads + 1, new ThreadFactory() {

                private final AtomicInteger threadNum = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("JaPS Server Thread #" + threadNum.getAndIncrement());

                    return thread;
                }
            });

            workerPool.execute(this);

            for (int i = 1; i <= workerThreads; i++) {
                SelectorThread selectorThread = new SelectorThread(i, selectorLock);
                selectorThreads.add(selectorThread);

                workerPool.execute(selectorThread);
            }

            selectorRoundRobin = new RoundRobinList<>(selectorThreads);

            LOGGER.log(Level.INFO, "JaPS server started on {0}:{1}", new Object[]{host, String.valueOf(port)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {

        LOGGER.info("Server will stop");

        if (serverSocketChannel != null) {
            try {
                serverSocketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Shutdown selector threads
        selectorThreads.forEach(SelectorThread::shutdown);

        // Close the pool
        workerPool.shutdown();

        // Close the cache
        cache.close();

        LOGGER.info("Server stopped!");
    }

    public void subscribeChannel(String channel, Connection connection) {

        if (channelSessions.containsKey(channel)) {
            channelSessions.get(channel).add(connection);
        } else {
            channelSessions.put(channel, new ArrayList<>(Collections.singletonList(connection)));
        }

        LOGGER.log(Level.FINE, "[{0}] Channel subscribed: {1}", new Object[]{connection.remoteAddress().toString(), channel});
    }

    public void unsubscribeChannel(String channel, Connection connection) {

        if (channelSessions.containsKey(channel)) {
            channelSessions.get(channel).remove(connection);

            LOGGER.log(Level.FINE, "[{0}] Channel unsubscribed: {1}", new Object[]{connection.remoteAddress().toString(), channel});
        }
    }

    public void removeClient(Connection connection) {

        for (String s : connection.channels()) {
            channelSessions.get(s).remove(connection);
        }

        if (!connection.channels().isEmpty()) {
            LOGGER.log(Level.FINE, "[{0}] Channels unsubscribed from {1}: {2}", new Object[]{connection.remoteAddress().toString(), connection.name(), String.join(", ", connection.channels())});
        }
    }

    public void broadcast(Connection con, String channel, String data) {

        if (channelSessions.containsKey(channel)) {
            for (Connection connection : channelSessions.get(channel)) {
                connection.send(data);
            }
        }

        // Broadcast it to the cluster if possible
        clusterBroadcast(con, channel, data);
    }

    public void broadcastTo(Connection con, String channel, JSONObject data, String subscriberName) {

        String clusterData = data.toString();

        if (channelSessions.containsKey(channel)) {
            // Remove the subscriber name to save bandwidth and remove the unneeded key
            data.remove("su");

            // Get the correct data to send
            String broadcastData = data.toString();

            // Find the subscribers with that name and route it to these
            for (Connection filteredConnection : channelSessions.get(channel).stream().filter(connection -> connection.name().equals(subscriberName)).collect(Collectors.toList())) {
                filteredConnection.send(broadcastData);
            }
        }

        // Broadcast it to the cluster if possible
        clusterBroadcast(con, channel, clusterData);
    }

    private void clusterBroadcast(Connection con, String channel, String data) {

        if (clusterPublisher.size() > 0) {
            JSONObject clusterMessage = new JSONObject(data)
                    .put("op", OpCode.OP_BROADCAST.getCode())
                    .put("ch", channel);

            // Publish it to all clusters but exclude the server which has sent it
            // if it comes from another JaPS server but also publish it
            // if a normal publisher client has sent it
            clusterPublisher.stream().filter(cl -> cl.connected && (con.host() == null || (con.host() != null && con.port() != cl.port && !con.host().equals(cl.host))))
                    .forEach(cl -> cl.write(clusterMessage));
        }
    }

    private void acquireLock() {

        selectorLock.lock();
    }

    private void releaseLock() {

        selectorLock.unlock();
    }

    public JaPSCache cache() {

        return cache;
    }

    @Override
    public void run() {

        while (serverSocketChannel.isOpen()) {
            try {
                if (selector.select() == 0) {
                    continue;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    keyIterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        // Accept the socket channel
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        if (socketChannel == null) {
                            continue;
                        }

                        // Configure non blocking and disable the Nagle algorithm
                        socketChannel.configureBlocking(false);
                        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

                        // Create new connection object
                        Connection connection = new Connection(this, socketChannel);

                        // Get a new selector thread based on round robin
                        SelectorThread nextSelector = selectorRoundRobin.next();
                        Selector sel = nextSelector.selector();

                        // Lock the selectors
                        acquireLock();

                        // Stop the new selector from hanging in select() call
                        sel.wakeup();

                        try {
                            // Register OP_READ and attach the connection object to it
                            socketChannel.register(sel, SelectionKey.OP_READ, connection);

                            LOGGER.log(Level.FINE, "[{0}] New connection, assigning selector {1}", new Object[]{socketChannel.getRemoteAddress(), nextSelector.id()});
                        } finally {
                            // Unlock the lock
                            releaseLock();
                        }
                    }
                }

                keys.clear();
            } catch (Exception e) {
                releaseLock();
                e.printStackTrace();
                break;
            }
        }
    }

    private static class ClusterPublisher {

        private SocketChannel socketChannel;

        private String host;

        private int port;

        private boolean connected;

        private AtomicBoolean reconnecting = new AtomicBoolean(false);

        public ClusterPublisher(String host, int port) {

            this.host = host;
            this.port = port;
        }

        private boolean connect() {

            try {
                socketChannel = SocketChannel.open();
                socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                socketChannel.connect(new InetSocketAddress(host, port));

                connected = true;

                new Thread(new KeepAliveTask()).start();

                return true;
            } catch (IOException e) {
                connected = false;
            }

            return false;
        }

        private void reconnect() {

            if (!reconnecting.get()){
                reconnecting.set(true);

                new Thread(new ConnectTask()).start();
            }
        }

        public void write(JSONObject jsonObject) {

            // Instantly return
            if (!connected) {
                return;
            }

            try {
                // Send the json data and prepend the length
                byte[] bytes = jsonObject.toString().getBytes("UTF-8");
                ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length + 4);
                byteBuffer.putInt(bytes.length);
                byteBuffer.put(bytes);

                // Prepare the buffer for writing
                byteBuffer.flip();

                socketChannel.write(byteBuffer);
            } catch (IOException e) {
                connected = false;

                try {
                    socketChannel.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                reconnect();

                LOGGER.log(Level.SEVERE, "Connection to cluster server {0}:{1} lost", new Object[] {host, String.valueOf(port)});
            }
        }

        private class ConnectTask implements Runnable {

            @Override
            public void run() {

                // Try to reconnect
                while (!connect()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LOGGER.log(Level.SEVERE, "Connected to cluster server {0}:{1}", new Object[] {host, String.valueOf(port)});

                reconnecting.set(false);

                connected = true;
            }
        }

        private class KeepAliveTask implements Runnable {

            @Override
            public void run() {

                while (connected) {
                    write(new JSONObject()
                            .put("op", OpCode.OP_KEEP_ALIVE.getCode())
                            .put("ch", "keep-alive"));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
