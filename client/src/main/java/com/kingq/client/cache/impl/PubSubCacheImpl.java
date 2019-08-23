package com.kingq.client.cache.impl;

import com.google.gson.Gson;
import com.kingq.client.cache.AsyncPubSubCache;
import com.kingq.client.cache.Cacheable;
import com.kingq.client.cache.PubSubCache;
import com.kingq.shared.config.ClusterServer;
import com.kingq.shared.net.OpCode;
import com.kingq.shared.nio.NioSocketClient;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by spring on 13.06.2017.
 */
public class PubSubCacheImpl extends NioSocketClient implements PubSubCache {

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
    public void clientReconnected() {

        // Not needed
    }

    @Override
    public void clientConnected() {

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
            case 11:
                int hasId = jsonObject.getInt("id");

                if (callbacks.containsKey(hasId)) {
                    // Catch user related exceptions extra
                    try {
                        // Remove the consumer and accept it
                        callbacks.remove(hasId).accept(new JSONObject().put("has", jsonObject.getBoolean("has")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return Collections.unmodifiableList(super.clusterServers());
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
    public void put(String key, JSONObject value, int expire) {

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
    public void put(String key, JSONObject value) {

        put(key, value, -1);
    }

    @Override
    public void putObject(String key, Object value, int expire) {

        if (value == null) {
            throw new IllegalArgumentException("value cannot be null or empty");
        }

        if (!(value instanceof Cacheable)) {
            throw new IllegalArgumentException("value must implement the 'Cacheable' class");
        }

        put(key, new JSONObject(gson.toJson(value)), expire);
    }

    @Override
    public void putObject(String key, Object value) {

        putObject(key, value, -1);
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
        callbacks.put(id, consumer);

        JSONObject jsonObject = new JSONObject()
                .put("op", OpCode.OP_CACHE_GET.getCode())
                .put("key", key)
                .put("id", id);

        write(jsonObject);
    }

    @Override
    public Future<Boolean> has(String key) {

        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        return executorService.submit(() -> {

            int id = CALLBACK_COUNTER.getAndIncrement();

            final boolean[] has = {false};

            CountDownLatch countDownLatch = new CountDownLatch(1);

            callbacks.put(id, new Consumer<JSONObject>() {

                @Override
                public void accept(JSONObject jsonObject) {

                    has[0] = jsonObject.getBoolean("has");

                    countDownLatch.countDown();
                }
            });

            JSONObject jsonObject = new JSONObject()
                    .put("op", OpCode.OP_CACHE_HAS.getCode())
                    .put("key", key)
                    .put("id", id);

            write(jsonObject);

            countDownLatch.await();

            return has[0];
        });
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


}
