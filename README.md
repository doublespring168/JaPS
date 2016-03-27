# JaPS
JaPS is a robust and lightweight Java Pub/Sub library which uses JSON.

I have started this project to learn myself how Pub/Sub libraries can work and it has turned out that JaPS works really well.

The publishing and subscribing of JSON messages are handled with channels. 
JaPS allows you to register handlers in which methods get invoked based on a key and value match. 
It is also possible that one handler is responsible for one channel and it's JSON messages.

# Installation

- Install [Maven 3](http://maven.apache.org/download.cgi)
- Clone/Download this repo
- Install it with: ```mvn clean install```

**Maven dependencies**

_Client:_
```xml
<dependency>
    <groupId>de.jackwhite20</groupId>
    <artifactId>japs-client</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

# Quick start

_Publisher:_
```java
Publisher publisher = PublisherFactory.create("localhost", 1337);

JSONObject fooBar = new JSONObject();
fooBar.put("foo", "bar");
publisher.publish("test", fooBar);

publisher.publish("gson", fooBar);

JSONObject backendJson = new JSONObject();
backendJson.put("role", "update");
backendJson.put("ping", 5);
publisher.publish("backend", backendJson);

```

_Subscriber:_
```java
Subscriber subscriber = SubscriberFactory.create("localhost", 1337);
subscriber.subscribe("test", TestChannelHandler.class);
subscriber.subscribeMulti("backend", BackendMultiChannelHandler.class);
subscriber.subscribe("gson", GsonChannelHandler.class);
```

_TestChannelHandler:_
```java
public class TestChannelHandler extends ChannelHandler<JSONObject> {

    @Override
    public void onMessage(String channel, JSONObject message) {

        System.out.println("TestChannelHandler: foo=" + message.get("foo"));
    }
}
```

_BackendMultiChannelHandler:_
```java
public class BackendMultiChannelHandler {

    @Key("role")
    @Value("update")
    public void onBackendRoleUpdate(JSONObject jsonObject) {

		// Keep in mind that the key (here "role") will be removed before invocation
        System.out.println("BMCH[role=update]: ping=" + jsonObject.getInt("ping"));
    }
}
```

_GsonChannelHandler:_
```java
public class GsonChannelHandler extends ChannelHandler<FooBar> {

    @Override
    public void onMessage(String channel, FooBar fooBar) {

        System.out.println("FooBar class: " + fooBar.toString());
    }
}
```

_The simple FooBar class:_
```java
public class FooBar {

    private String foo;

    @Override
    public String toString() {

        return "FooBar{" +
                "foo='" + foo + '\'' +
                '}';
    }
}
```

### License

Licensed under the GNU General Public License, Version 3.0.
