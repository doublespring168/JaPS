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

JSONObject jsonObject = new JSONObject();
jsonObject.put("foo", "bar");
publisher.publish("test", jsonObject);
```

### License

Licensed under the GNU General Public License, Version 3.0.
