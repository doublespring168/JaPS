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

package de.jackwhite20.japs.server.config;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class Config {

    private String host;

    private int port;

    private int backlog = 50;

    private boolean debug = true;

    private int workerThreads = 4;

    public Config(String host, int port, int backlog, boolean debug, int workerThreads) {

        this.host = host;
        this.port = port;
        this.backlog = backlog;
        this.debug = debug;
        this.workerThreads = workerThreads;
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

    @Override
    public String toString() {

        return "Config{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", backlog=" + backlog +
                ", debug=" + debug +
                ", workerThreads=" + workerThreads +
                '}';
    }
}
