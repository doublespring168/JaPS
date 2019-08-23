import com.kingq.client.sub.impl.handler.ChannelHandler;
import com.kingq.client.sub.impl.handler.annotation.Channel;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by spring on 25.03.2017.
 */
@Channel("test")
public class TestChannelHandler extends ChannelHandler<JSONObject> {

    public static final AtomicInteger PACKETS = new AtomicInteger(1);

    @Override
    public void onMessage(String channel, JSONObject message) {

        System.out.println("TestChannelHandler: foo=" + message.get("foo"));

        //if(packets.get() % 50 == 0) {
        //System.err.println("Messages: " + packets.get() + "/" + PublisherTest.TOTAL_MESSAGES);
        //}

        PACKETS.incrementAndGet();

        if (PACKETS.get() == PublisherTest.TOTAL_MESSAGES) {
            System.err.println("ALL RECEIVED");
        }
    }
}
