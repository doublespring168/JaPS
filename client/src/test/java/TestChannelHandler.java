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

import de.jackwhite20.japs.client.sub.impl.handler.ChannelHandler;
import de.jackwhite20.japs.client.sub.impl.handler.annotation.Channel;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by JackWhite20 on 25.03.2016.
 */
@Channel("test")
public class TestChannelHandler extends ChannelHandler<JSONObject> {

    public static final AtomicInteger PACKETS = new AtomicInteger(1);

    @Override
    public void onMessage(String channel, JSONObject message) {

        System.out.println("TestChannelHandler: foo=" + message.get("foo"));

        //if(packets.get() % 50 == 0) {
        //System.err.println("Messages: " + packets.get() + "/" + PublisherTest.TOTAL_MESSAGES);
        //}

        PACKETS.incrementAndGet();

        if (PACKETS.get() == PublisherTest.TOTAL_MESSAGES) {
            System.err.println("ALL RECEIVED");
        }
    }
}
