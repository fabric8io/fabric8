## amq: endpoint

The **amq:** endpoint works exactly like the **[activemq:](http://camel.apache.org/activemq.html)** endpoint in Apache Camel; only it uses the fabric to automatically discover the broker. So there is no configuration required; it'll just work out of the box and automatically discover whatever ActiveMQ message brokers are available; with failover and load balancing.

So you use it like this from Camel routes:

```
from("amq:MyQueue").to("amq:topic:MyTopic");
```

### Using broker groups

In addition to auto discovery and load balancing of ActiveMQ brokers, the **amq:** endpoint works great with [Broker Groups](brokerTopology.html).

A great use case for broker groups is if you want to have data centre or geographic clusters of brokers. e.g. one for the US, one for Europe and one for Asia. Then fabric8 automatically creates a [Profile](profiles.html) for each mq client in each group. (e.g. mq-client-us, mq-client-europe, mq-client-asia).

Then by just adding the profile for the broker group you wish to connect to to your container (e.g. "mq-client-europe") then the **amq:** endpoint will automatically connect to that broker group ("Europe").

See the [Broker Clients documentation](brokerClients.html) for more details.
