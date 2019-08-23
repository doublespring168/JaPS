package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;

/**
 * Created by spring on 05.05.2017.
 */
public class EndCommand extends Command {

    public EndCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        KingQ.getInstance().stop();

        return true;
    }
}
