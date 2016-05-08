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

import de.jackwhite20.japs.client.pub.Publisher;
import de.jackwhite20.japs.client.pub.PublisherFactory;
import de.jackwhite20.japs.client.sub.Subscriber;
import de.jackwhite20.japs.client.sub.SubscriberFactory;
import org.json.JSONObject;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
public class PublisherTest {

    public static final String HOST = "192.168.2.102";
    public static final int CLIENTS = 20;
    public static final int MESSAGES_PER_CLIENT = 100;
    public static final int TOTAL_MESSAGES = CLIENTS * MESSAGES_PER_CLIENT;

    public static void main(String[] args) {

        Subscriber subscriber = SubscriberFactory.create(HOST, 6000);
        subscriber.subscribe(TestChannelHandler.class);
        subscriber.subscribe(TestGsonChannelHandler.class);
        subscriber.subscribeMulti(BackendMultiChannelHandler.class);

/*        CountDownLatch countDownLatch = new CountDownLatch(CLIENTS);

        for (int i = 0; i < CLIENTS; i++) {
            new Thread(() -> {

                Publisher publisher = PublisherFactory.create(HOST, 1337);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("foo", "bar");

                for (int j = 0; j < MESSAGES_PER_CLIENT; j++) {
                    publisher.publish("test", jsonObject);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                publisher.disconnect();

                countDownLatch.countDown();
            }).start();
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Test finished successfully! " + TestChannelHandler.PACKETS +  "/" + TOTAL_MESSAGES);

        subscriber.disconnect();*/

        Publisher publisher = PublisherFactory.create(HOST, 6000);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("foo", "bar");
        publisher.publish("test", jsonObject);

        //publisher.publish("gson", new FooBar("bar"));

        JSONObject backendJson = new JSONObject();
        backendJson.put("role", "update");
        backendJson.put("ping", 5);
        publisher.publish("backend", backendJson);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        subscriber.disconnect(true);
        publisher.disconnect(true);
    }
}
