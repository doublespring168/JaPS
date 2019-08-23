package com.kingq.server.command.impl;

import com.kingq.client.sub.Subscriber;
import com.kingq.server.command.Command;
import com.kingq.server.command.impl.sub.SubCommand;

/**
 * Created by spring on 05.05.2017.
 */
public class UnsubCommand extends Command {

    public UnsubCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        for (Subscriber subscriber : SubCommand.SUBSCRIBERS) {
            subscriber.disconnect(true);
        }

        return true;
    }
}
