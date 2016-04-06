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

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by JackWhite20 on 06.04.2016.
 */
public class SelectorThread implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(SelectorThread.class.getName());

    private boolean running;

    private int id;

    private Selector selector;

    private ReentrantLock selectorLock;

    public SelectorThread(int id, ReentrantLock selectorLock) {

        this.id = id;
        this.selectorLock = selectorLock;

        LOGGER.log(Level.FINE, "Selector thread {0} started!", id);
    }

    public int id() {

        return id;
    }

    public Selector selector() {

        return selector;
    }

    public void shutdown() {

        running = false;

        if(selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        try {
            this.selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Failed to open Selector " + id + "!");
        }

        running = true;

        while (running) {
            try {
                selectorLock.lock();
                selectorLock.unlock();

                if (selector.select() == 0)
                    continue;

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    keyIterator.remove();

                    SelectableChannel selectableChannel = key.channel();

                    if(!key.isValid())
                        continue;

                    if (key.isReadable()) {
                        Connection connection = (Connection) key.attachment();

                        if(connection == null)
                            continue;

                        if(selectableChannel instanceof DatagramChannel) {
                            // TODO: 06.04.2016
                        }else {
                            connection.read();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
