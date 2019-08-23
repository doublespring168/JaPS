package com.kingq.client.pub.impl;

import com.kingq.client.pub.AsyncPublisher;
import com.kingq.client.pub.Publisher;
import com.kingq.shared.config.ClusterServer;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by spring on 07.05.2017.
 */
public class AsyncPublisherImpl implements AsyncPublisher {

    private ExecutorService executorService;

    private Publisher publisher;

    public AsyncPublisherImpl(ExecutorService executorService, Publisher publisher) {

        this.executorService = executorService;
        this.publisher = publisher;
    }

    @Override
    public ExecutorService executorService() {

        return executorService;
    }

    @Override
    public void disconnect(boolean force) {

        publisher.disconnect(force);
    }

    @Override
    public void disconnect() {

        disconnect(true);
    }

    @Override
    public void publish(String channel, JSONObject jsonObject) {

        executorService.execute(() -> publisher.publish(channel, jsonObject));
    }

    @Override
    public void publishAll(String channel, JSONObject... jsonObjects) {

        executorService.execute(() -> publisher.publish(channel, jsonObjects));
    }

    @Override
    public void publish(String channel, String subscriberName, JSONObject jsonObject) {

        executorService.execute(() -> publisher.publish(channel, subscriberName, jsonObject));
    }

    @Override
    public void publishAll(String channel, String subscriberName, JSONObject... jsonObjects) {

        executorService.execute(() -> publisher.publishAll(channel, subscriberName, jsonObjects));
    }

    @Override
    public void publish(String channel, String json) {

        executorService.execute(() -> publisher.publish(channel, json));
    }

    @Override
    public void publish(String channel, String json, String subscriberName) {

        executorService.execute(() -> publisher.publish(channel, json, subscriberName));
    }

    @Override
    public void publish(String channel, Object object) {

        executorService.execute(() -> publisher.publish(channel, object));
    }

    @Override
    public void publishAll(String channel, Object... objects) {

        executorService.execute(() -> publisher.publishAll(channel, objects));
    }

    @Override
    public void publish(String channel, String subscriberName, Object object) {

        executorService.execute(() -> publisher.publish(channel, subscriberName, object));
    }

    @Override
    public void publishAll(String channel, String subscriberName, Object... objects) {

        executorService.execute(() -> publisher.publishAll(channel, subscriberName, objects));
    }

    @Override
    public boolean connected() {

        return publisher.connected();
    }

    @Override
    public AsyncPublisher async() {

        return this;
    }

    @Override
    public List<ClusterServer> clusterServers() {

        return publisher.clusterServers();
    }


}
