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

package de.jackwhite20.japs.client.cache.impl;

import com.google.gson.Gson;
import de.jackwhite20.japs.client.cache.AsyncPubSubCache;
import de.jackwhite20.japs.client.cache.Cacheable;
import de.jackwhite20.japs.client.cache.PubSubCache;
import de.jackwhite20.japs.shared.config.ClusterServer;
import de.jackwhite20.japs.shared.net.OpCode;
import de.jackwhite20.japs.shared.nio.NioSocketClient;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by JackWhite20 on 13.06.2016.
 */
public class PubSubCacheImpl extends NioSocketClient implements PubSubCache, Runnable {

    private static final AtomicInteger CALLBACK_COUNTER = new AtomicInteger(0);

    private Map<Integer, Consumer> callbacks = new ConcurrentHashMap<>();

    private Gson gson = new Gson();

    private ExecutorService executorService;

    private AsyncPubSubCache asyncPubSubCache;

    public PubSubCacheImpl(String host, int port) {

        this(Collections.singletonList(new ClusterServer(host, port)));
    }

    public PubSubCacheImpl(List<ClusterServer> clusterServers) {

        super(clusterServers);

        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("PubSubCache Thread");

            return thread;
        });
        this.asyncPubSubCache = new AsyncPubSubCacheImpl(executorService, this);
    }

    @Override
    public void clientConnected() {

        // Not needed
    }

    @Override
    public void clientReconnected() {

        // Not needed
    }

    @SuppressWarnings("unchecked")
    @Override
    public void received(JSONObject jsonObject) {

        int op = ((Integer) jsonObject.remove("op"));

        switch (op) {
            case 5:
                int id = jsonObject.getInt("id");
                Object value = (jsonObject.has("value")) ? jsonObject.get("value") : null;

                if (callbacks.containsKey(id)) {
                    // Catch user related exceptions extra
                    try {
                        // Remove the consumer and accept it
                        callbacks.remove(id).accept(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public void disconnect(boolean force) {

        close(force);

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public void put(String key, String value, int expire) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_ADD.getCode())
                .put("key", key)
                .put("value", value)
                .put("expire", expire);

        write(jsonObject);
    }

    @Override
    public void put(String key, String value) {

        put(key, value, -1);
    }

    @Override
    public void put(String key, JSONObject value, int expire) {

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        put(key, value.toString(), expire);
    }

    @Override
    public void put(String key, JSONObject value) {

        put(key, value, -1);
    }

    @Override
    public void put(String key, Object value, int expire) {

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        if (!(value instanceof Cacheable)) {
            throw new IllegalArgumentException("value must implement the 'Cacheable' class");
        }

        put(key, gson.toJson(value), expire);
    }

    @Override
    public void put(String key, Object value) {

        put(key, value, -1);
    }

    @Override
    public void expire(String key, int secondsToLive) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_SET_EXPIRE.getCode())
                .put("key", key)
                .put("expire", (secondsToLive > 0) ? secondsToLive : -1);

        write(jsonObject);
    }

    @Override
    public void expire(String key, Consumer<Integer> consumer) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        callbacks.put(id, consumer);

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET_EXPIRE.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public void get(String key, Consumer<JSONObject> consumer) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        // TODO: 14.06.2016 Maybe improve
        callbacks.put(id, new Consumer<String>() {

            @Override
            public void accept(String o) {

                consumer.accept(new JSONObject(o));
            }
        });

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public <T> void getClass(String key, Consumer<T> consumer, Class<T> clazz) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        if (consumer == null) {
            throw new IllegalArgumentException("consumer cannot be null or empty");
        }

        if (clazz == null) {
            throw new IllegalArgumentException("clazz cannot be null or empty");
        }

        int id = CALLBACK_COUNTER.getAndIncrement();

        // TODO: 14.06.2016 Maybe improve
        // Get the class as string and deserialize it
        callbacks.put(id, new Consumer<String>() {

            @Override
            public void accept(String s) {

                consumer.accept(gson.fromJson(s, clazz));
            }
        });

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public void remove(String key) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_REMOVE.getCode())
                .put("key", key);

        write(jsonObject);
    }

    @Override
    public AsyncPubSubCache async() {

        return asyncPubSubCache;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
    }
}
