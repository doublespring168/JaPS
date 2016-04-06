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

import de.jackwhite20.japs.server.config.Config;
import de.jackwhite20.japs.server.util.RoundRobinList;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class JaPSServer implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(JaPSServer.class.getName());

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

    private AtomicInteger selectorCounter = new AtomicInteger(0);

    private int workerThreads;

    public JaPSServer(String host, int port, int backlog, boolean debug, int workerThreads) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.workerThreads = workerThreads;

        Logger.getLogger("de.jackwhite20").setLevel((debug) ? Level.FINE : Level.INFO);

        start();
    }

    public JaPSServer(Config config) {

        this(config.host(), config.port(), config.backlog(), config.debug(), config.workerThreads());
    }

    private void start() {

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(host, port), backlog);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            workerPool = Executors.newFixedThreadPool(workerThreads + 1);

            workerPool.execute(this);

            for (int i = 1; i <= workerThreads; i++) {
                SelectorThread selectorThread = new SelectorThread(i, selectorLock);
                selectorThreads.add(selectorThread);

                workerPool.execute(selectorThread);
            }

            selectorRoundRobin = new RoundRobinList<>(selectorThreads);

            LOGGER.log(Level.INFO, "JaPS server started on {0}:{1}", new Object[] {host, String.valueOf(port)});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subscribeChannel(String channel, Connection connection) {

        if(channelSessions.containsKey(channel)) {
            channelSessions.get(channel).add(connection);
        } else {
            channelSessions.put(channel, new ArrayList<>(Collections.singletonList(connection)));
        }

        LOGGER.log(Level.FINE, "[{0}] Channel subscribed: {1}", new Object[] {connection.remoteAddress().toString(), channel});
    }

    public void unsubscribeChannel(String channel, Connection connection) {

        if(channelSessions.containsKey(channel)) {
            channelSessions.get(channel).remove(connection);

            LOGGER.log(Level.FINE, "[{0}] Channel unsubscribed: {1}", new Object[] {connection.remoteAddress().toString(), channel});
        }
    }

    public void removeClient(Connection connection) {

        for (String s : connection.channels()) {
            channelSessions.get(s).remove(connection);
        }

        if(!connection.channels().isEmpty()) {
            LOGGER.log(Level.FINE, "[{0}] Channels unsubscribed: {1}", new Object[] {connection.remoteAddress().toString(), String.join(", ", connection.channels())});
        }
    }

    public void broadcast(String channel, String data) {

        if(channelSessions.containsKey(channel)) {
            channelSessions.get(channel).forEach(session -> session.send(data));
        }
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

                    if(!key.isValid()) {
                        continue;
                    }

                    if(key.isAcceptable()) {
                        // Accept the socket channel
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        if(socketChannel == null) {
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
                        selectorLock.lock();

                        // Stop the new selector from hanging in select() call
                        sel.wakeup();

                        try {
                            // Register OP_READ and attach the connection object to it
                            socketChannel.register(sel, SelectionKey.OP_READ, connection);
                        } finally {
                            // Unlock the lock
                            selectorLock.unlock();
                        }

                        LOGGER.log(Level.FINE, "[{0}] New connection, assigning selector {1}", new Object[] {socketChannel.getRemoteAddress(), nextSelector.id()});
                    }
                }

                keys.clear();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
