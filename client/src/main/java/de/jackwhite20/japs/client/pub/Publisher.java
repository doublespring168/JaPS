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

import org.json.JSONObject;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public interface Publisher {

    /**
     * Disconnects the publisher
     */
    void disconnect();

    /**
     * Publishes the given JSONObject to the given channel.
     *
     * @param channel The channel.
     * @param jsonObject The json as JSONObject.
     */
    void publish(String channel, JSONObject jsonObject);

    /**
     * Publishes the given JSONObject to the given channel.
     *
     * @param channel The channel.
     * @param json The raw json string.
     */
    void publish(String channel, String json);

    /**
     * Returns if the publisher is connected.
     *
     * @return True if connected, otherwise false.
     */
    boolean connected();
}
