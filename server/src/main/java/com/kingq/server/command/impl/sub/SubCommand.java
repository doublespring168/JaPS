package com.kingq.server.command.impl.sub;

import com.kingq.client.sub.Subscriber;
import com.kingq.client.sub.SubscriberFactory;
import com.kingq.server.KingQ;
import com.kingq.server.command.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by spring on 05.05.2017.
 */
public class SubCommand extends Command {

    public static List<Subscriber> SUBSCRIBERS = new ArrayList<>();

    public SubCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length == 1) {
            Subscriber subscriber = SubscriberFactory.create("localhost", KingQ.getConfig().port());
            subscriber.subscribe(args[0], SubCommandChannelHandler.class);

            SUBSCRIBERS.add(subscriber);

            KingQ.getLogger().info("Subscribed channel " + args[0]);
        } else {
            KingQ.getLogger().info("Usage: sub <Channel>");
        }

        return true;
    }
}
