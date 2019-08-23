package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;

/**
 * Created by spring on 24.06.2017.
 */
public class SnapshotCommand extends Command {

    public SnapshotCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length == 0) {
            KingQ.getLogger().info("Usage: snapshot create | snapshot load <Path>");
            return false;
        }

        if (args[0].equalsIgnoreCase("create")) {
            KingQ.getServer().cache().snapshot();
        } else if (args[0].equalsIgnoreCase("load")) {
            KingQ.getServer().cache().loadSnapshot(args[1]);
        }

        return true;
    }
}
