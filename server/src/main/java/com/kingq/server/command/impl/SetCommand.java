package com.kingq.server.command.impl;

import com.kingq.server.KingQ;
import com.kingq.server.command.Command;
import com.kingq.shared.net.OpCode;
import org.json.JSONObject;

/**
 * Created by spring on 22.06.2017.
 */
public class SetCommand extends Command {

    public SetCommand(String name, String[] aliases, String description) {

        super(name, aliases, description);
    }

    @Override
    public boolean execute(String[] args) {

        if (args.length < 2) {
            KingQ.getLogger().info("Usage: set <Key> [Expire] <Value>");
            return false;
        }

        int expire = -1;
        String value = args[1];

        if (args.length >= 3) {
            try {
                expire = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                KingQ.getLogger().info("'" + args[1] + "' is not a number");
                return false;
            }

            String[] values = new String[args.length - 2];
            System.arraycopy(args, 2, values, 0, args.length - 2);

            value = String.join(" ", values);
        }

        KingQ.getServer().cache().put(args[0], value);

        KingQ.getServer().clusterBroadcast(null, new JSONObject()
                .put("op", OpCode.OP_CACHE_ADD.getCode())
                .put("key", args[0])
                .put("value", value)
                .put("expire", expire));

        return true;
    }
}
