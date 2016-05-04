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

package de.jackwhite20.japs.client.pub;

import de.jackwhite20.japs.client.pub.impl.PublisherImpl;

/**
 * Created by JackWhite20 on 25.03.2016.
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

        return new PublisherImpl(host, port);
    }
}
