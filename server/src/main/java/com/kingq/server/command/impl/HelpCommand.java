package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;

/**
 * Created by spring on 17.07.2015.
 */
public class HelpCommand extends Command {

    public HelpCommand(String name, String[] aliases, String description) {
        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        KingQ.getLogger().info("Available Commands:");
        for (Command command : KingQ.getCommandManager().getCommands()) {
            KingQ.getLogger().info(command.getName() + " [" + String.join(", ", command.getAliases()) + "] - " + command.getDescription());
        }

        return true;
    }
}
