package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;
import com.kingq.shared.net.OpCode;
import org.json.JSONObject;

/**
 * Created by spring on 24.06.2017.
 */
public class DeleteCommand extends Command {

    public DeleteCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length != 1) {
            KingQ.getLogger().info("Usage: delete <Key>");
            return false;
        }

        String key = args[0];

        KingQ.getServer().cache().remove(key);

        // Manually broadcast it to the cluster
        KingQ.getServer().clusterBroadcast(null, new JSONObject()
                .put("op", OpCode.OP_CACHE_REMOVE.getCode())
                .put("key", key));

        return true;
    }
}
