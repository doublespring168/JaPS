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

package de.jackwhite20.japs.client.pub.impl;

import de.jackwhite20.japs.client.pub.AsyncPublisher;
import de.jackwhite20.japs.client.pub.Publisher;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;

/**
 * Created by JackWhite20 on 07.05.2016.
 */
public class AsyncPublisherImpl implements AsyncPublisher {

    private ExecutorService executorService;

    private Publisher publisher;

    public AsyncPublisherImpl(ExecutorService executorService, Publisher publisher) {

        this.executorService = executorService;
        this.publisher = publisher;
    }

    @Override
    public void disconnect(boolean force) {

        publisher.disconnect(force);
    }

    @Override
    public void publish(String channel, JSONObject jsonObject) {

        executorService.execute(() -> publisher.publish(channel, jsonObject));
    }

    @Override
    public void publish(String channel, JSONObject jsonObject, String subscriberName) {

        executorService.execute(() -> publisher.publish(channel, jsonObject, subscriberName));
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
    public void publish(String channel, Object object, String subscriberName) {

        executorService.execute(() -> publisher.publish(channel, object, subscriberName));
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
    public ExecutorService executorService() {

        return executorService;
    }
}
