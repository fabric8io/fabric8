## Broker Clients

If you create [A-MQ message broker topologies](http://www.jboss.org/products/amq) via the [Fuse Fabric CLI, JMX API or Fuse Management Console](brokerTopology.md) then you can use the **mq-fabric** module's fabric discovery mechanism to find one of the brokers in the group of brokers to connect to.

However to make this process even easier; when you create a broker group, Fuse Fabric automaically creates a _mq client profile_ to connect to the broker group. All this profile really does is define the broker group in OSGI confg admin; so that common clients connect to the right broker.

### JMS Clients

If you are a JMS client then you can use the **mq-fabric-cf** module or feature; which creates an MQ Fabric based ActiveMQConnectionFactory object which is pre-configured with the group name so that it automatically connects to the correct broker group.

So all you need to do is lookup the ActiveMQConnectionFactory object in the OSGi regstry; or have it injected into you via SCR annotations; and you're good to go.

For example the **exmaple-mq** profile uses this approach to [inject the connection factory](https://github.com/jboss-fuse/fuse/blob/master/fabric/fabric-examples/fabric-activemq-demo/src/main/java/org/fusesource/fabric/demo/activemq/ActiveMQConsumerFactory.java#L39) which then connects to the correct broker.

If you create a few broker groups vi the [Fuse Shell or Fuse Management Console](brokerTopology.md), for example imagine you create groups "us" and "emea"; then you could deploy the profiles

* example-mq and mq-client-us to connect the example-mq to the **us** broker group
* example-mq and mq-client-emea to connect the example-mq to the **emea** broker group

### Camel clients

The **mq-fabric-camel** defines an **amq** camel component which works just like the **activemm** camel component only it depends on an ActiveMQConnectionFactory being injected from the OSGi registry (and refuses to start until one is available). So like above we can use the mq-client profiles to combine with the **mq-fabric-camel** model and the **amq* component to work with A-MQ brokers.

For example the **example-camel-mq** profile is a simple profile defining a camel route in its [camel.xml](https://github.com/jboss-fuse/fuse/blob/master/fabric/fuse-fabric/src/main/resources/distro/fabric/import/fabric/configs/versions/1.0/profiles/example-camel-mq/camel.xml) which uses the **amq:** endpoints to work with [Apache ActiveMQ]

If you deploy it with a client profile then it will connect to that client profile's broker group. e.g.

* example-camel-mq and mq-client-us to connect the example-camel-mq to the **us** broker group
* example-camel-mq and mq-client-emea to connect the example-camel-mq to the **emea** broker group
