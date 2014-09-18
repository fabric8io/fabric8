## master: endpoint

The **master:** endpoint provides a way to ensure only a single consumer in a cluster consumes from a given endpoint; with automatic failover if that JVM dies.

This can be very useful if you need to consume from some legacy back end which either doesn't support concurrent consumption or due to commercial or stability reasons you can only have a single connection at any point in time.

### Using the master endpoint

Just prefix any camel endpoint with **master:someName:** where _someName_ is a logical name and is used to acquire the master lock. e.g.

```
from("master:cheese:jms:foo").to("activemq:wine");
```
The above simulates the [Exclusive Consumers](http://activemq.apache.org/exclusive-consumer.html) type feature in ActiveMQ; but on any third party JMS provider which maybe doesn't support exclusive consumers.
