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

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
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

    public JaPSServer(String host, int port, int backlog) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;

        start();
    }

    public JaPSServer(Config config) {

        this(config.host(), config.port(), config.backlog());
    }

    private void start() {

        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(host, port), backlog);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            new Thread(this).start();

            LOGGER.log(Level.INFO, "JaPS server started on {0}:{1}", new Object[] {host, String.valueOf(port)});
        } catch (Exception e) {
            e.printStackTrace();
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
                        SocketChannel socketChannel = serverSocketChannel.accept();

                        if(socketChannel == null) {
                            continue;
                        }

                        socketChannel.configureBlocking(false);
                        socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

                        LOGGER.log(Level.INFO, "New connection from {0}", socketChannel.getRemoteAddress());

                        // TODO: 25.03.2016
                    }

                    if(key.isReadable()) {
                        // TODO: 25.03.2016  
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
