package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;

/**
 * Created by spring on 22.06.2017.
 */
public class GetCommand extends Command {

    public GetCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length != 1) {
            KingQ.getLogger().info("Usage: get <Key>");
            return false;
        }

        String key = args[0];

        System.out.println(key + "=" + KingQ.getServer().cache().get(key));

        return true;
    }
}
