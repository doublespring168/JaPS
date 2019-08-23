import com.kingq.client.sub.impl.handler.ChannelHandler;
import com.kingq.client.sub.impl.handler.annotation.Channel;

/**
 * Created by spring on 27.03.2017.
 */
@Channel("gson")
public class TestGsonChannelHandler extends ChannelHandler<FooBar> {

    @Override
    public void onMessage(String channel, FooBar message) {

        System.out.println("FooBar class: " + message.toString());
    }
}
