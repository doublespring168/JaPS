import com.kingq.client.sub.impl.handler.annotation.Channel;
import com.kingq.client.sub.impl.handler.annotation.Key;
import com.kingq.client.sub.impl.handler.annotation.Value;
import org.json.JSONObject;

/**
 * Created by spring on 25.03.2017.
 */
@Channel("backend")
public class BackendMultiChannelHandler {

    @Key("role")
    @Value("update")
    public void onBackendRoleUpdate(JSONObject jsonObject) {

        // Keep in mind that the key (here "role") will be removed before invocation
        System.out.println("BMCH[role=update]: ping=" + jsonObject.getInt("ping"));
    }

    @Key("role")
    @Value("delete")
    public void onBackendRoleDelete(FooBar fooBar) {

        System.out.println("FooBar[role=delete]: " + fooBar.toString());
    }
}
