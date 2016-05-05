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

package de.jackwhite20.japs.server.command.impl.sub;

import de.jackwhite20.japs.client.sub.Subscriber;
import de.jackwhite20.japs.client.sub.SubscriberFactory;
import de.jackwhite20.japs.server.JaPS;
import de.jackwhite20.japs.server.command.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JackWhite20 on 05.05.2016.
 */
public class SubCommand extends Command {

    public static List<Subscriber> SUBSCRIBERS = new ArrayList<>();

    public SubCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length == 1) {
            Subscriber subscriber = SubscriberFactory.create("localhost", 6000);
            subscriber.subscribe(args[0], SubCommandChannelHandler.class);

            SUBSCRIBERS.add(subscriber);

            JaPS.getLogger().info("Subscribed channel " + args[0]);
        } else {
            JaPS.getLogger().info("Usage: sub <Channel>");
        }

        return true;
    }
}
