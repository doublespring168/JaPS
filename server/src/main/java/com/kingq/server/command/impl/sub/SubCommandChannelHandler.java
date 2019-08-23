package com.kingq.server.command.impl.sub;

import com.kingq.client.sub.impl.handler.ChannelHandler;
import com.kingq.server.KingQ;
import org.json.JSONObject;

/**
 * Created by spring on 05.05.2017.
 */
public class SubCommandChannelHandler extends ChannelHandler<JSONObject> {

    @Override
    public void onMessage(String channel, JSONObject message) {

        KingQ.getLogger().info("[" + channel + "] " + message);
    }
}
