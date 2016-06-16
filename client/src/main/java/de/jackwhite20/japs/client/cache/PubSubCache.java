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

package de.jackwhite20.japs.client.cache;

import org.json.JSONObject;

import java.util.function.Consumer;

/**
 * Created by JackWhite20 on 13.06.2016.
 */
public interface PubSubCache {

    /**
     * Disconnects the pub sub cache client.
     */
    void disconnect();

    /**
     * Puts a key with its string value and the expire.
     *
     * @param key The key.
     * @param value The value.
     * @param expire The expire in seconds.
     */
    void put(String key, String value, int expire);

    /**
     * Puts a key with its string value.
     * This key will not expire!
     *
     * @param key The key.
     * @param value The value.
     */
    void put(String key, String value);

    /**
     * Puts a key with its json object and the expire.
     *
     * @param key The key.
     * @param value The value.
     * @param expire The expire in seconds.
     */
    void put(String key, JSONObject value, int expire);

    /**
     * Puts a key with its json object.
     * This key will not expire!
     *
     * @param key The key.
     * @param value The value.
     */
    void put(String key, JSONObject value);

    /**
     * Puts a key with its object value and the expire.
     *
     * @param key The key.
     * @param value The value.
     * @param expire The expire in seconds.
     */
    void put(String key, Object value, int expire);

    /**
     * Puts a key with its object value.
     * This key will not expire!
     *
     * @param key The key.
     * @param value The value.
     */
    void put(String key, Object value);

    /**
     * Gets the value associated with that key and calls the consumer with the value.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    void get(String key, Consumer<JSONObject> consumer);

    /**
     * Gets the value as a class object and calls the consumer with this class.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    <T> void getClass(String key, Consumer<T> consumer, Class<T> clazz);

    /**
     * Removes a key and its value from the pub sub cache.
     *
     * @param key The key to remove.
     */
    void remove(String key);

    /**
     * Sets the expire time in seconds for the given key.
     *
     * @param key the key.
     * @param secondsToLive expire time in seconds.
     */
    void expire(String key, int secondsToLive);

    /**
     * Gets the time how long the key lives until it's expired.
     *
     * @param key The key.
     * @param consumer The consumer.
     */
    void expire(String key, Consumer<Integer> consumer);

    /**
     * Returns the async pub sub cache implementation.
     *
     * @return The async pub sub cache.
     */
    AsyncPubSubCache async();
}
