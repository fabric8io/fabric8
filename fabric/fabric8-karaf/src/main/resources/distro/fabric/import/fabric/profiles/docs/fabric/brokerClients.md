## Broker Clients

If you create [A-MQ message broker topologies](http://www.jboss.org/products/amq) via the [Fabric8 CLI, JMX API or Fuse Management Console](brokerTopology.md) then you can use the **mq-fabric** module's fabric discovery mechanism to find one of the brokers in the group of brokers to connect to.

However to make this process even easier; when you create a broker group, Fabric8 automatically creates a _mq client profile_ to connect to the broker group. All this profile really does is define the broker group in OSGI confg admin; so that common clients connect to the right broker.

### JMS Clients

If you are a JMS client then you can use the **mq-fabric** module or feature; which creates an MQ Fabric based ActiveMQConnectionFactory object which is pre-configured with the group name so that it automatically connects to the correct broker group.

So all you need to do is lookup the ActiveMQConnectionFactory object in the OSGi regstry; or have it injected into you via SCR annotations; and you're good to go.

Fabric comes with two profiles **example-mq-producer** and **example-mq-consumer**, which can be used to send and receive messages from the brokers. Or you can use **example-mq** profile which starts both producer and consumer in the same container. These profile uses SCR approach to [inject the connection factory](https://github.com/jboss-fuse/fuse/blob/master/fabric/fabric-examples/fabric-activemq-demo/src/main/java/io/fabric8/demo/activemq/ActiveMQConsumerFactory.java#L39) which then connects to the correct broker.

Every time you create a profile for a broker group, a respective client profile for connection factory settings needed to connect to that group will be created. Imagine that you create **us** and **emea** groups with the [Fuse Shell or Fuse Management Console](brokerTopology.md).
Then profiles **mq-client-us** and **mq-client-emea** will be also created. Then, for example, you could deploy the profiles

* **example-mq-producer** and **mq-client-us** to connect the producer to the **us** broker group , or
* **example-mq-consumer** and **mq-client-emea** to connect the consumer to the **emea** broker group

Here's a step-by-step example that demonstrates how to create and connect to different broker groups.

1. First we need to create a fabric (if you don't have one already running):

        FuseFabric:karaf@root> fabric:create --new-user=admin --new-user-password=admin --wait-for-provisioning
        Waiting for container root to provision.
        Using specified zookeeper password:admin

2. Now let's create two containers **node-us** which will host broker in group **us** and **node-emea** for broker in **emea** group

        FuseFabric:karaf@root> container-create-child root node-us
        The following containers have been created successfully:
	    Container: node-us.

        FuseFabric:karaf@root> container-create-child root node-emea
        The following containers have been created successfully:
	    Container: node-emea.


3. When containers are ready, we can create appropriate broker profiles and assign them to the containers

        FuseFabric:karaf@root> mq-create --assign-container node-us --group us us
        MQ profile mq-broker-us.us ready

        FuseFabric:karaf@root> mq-create --assign-container node-emea --group emea emea
        MQ profile mq-broker-emea.emea ready

    We can see that profile **mq-broker-emea.emea** and **mq-broker-us.us** is assigned to **node-emea** and **node-us** nodes respectively.


        FuseFabric:karaf@root> container-list
        [id]                           [version] [alive] [profiles]                                         [provision status]
        root*                        1.0       true    fabric, fabric-ensemble-0000-1                     success
        node-emea                    1.0       true    default, mq-broker-emea.emea                       success
        node-us                      1.0       true    default, mq-broker-us.us                           success

    Also, that our brokers are running in appropriate groups

        FuseFabric:karaf@root> cluster-list
        [cluster]                      [masters]                      [slaves]                       [services]
        fusemq/emea
           emea                        node-emea                      -                              tcp://local:64023
        fusemq/us
           us                          node-us                        -                              tcp://local:63986

    Now let's take a look at client profiles that are created for connecting to the brokers

        FuseFabric:karaf@root> profile-list | grep mq-client
        feature-camel-jms                        0              feature-camel, mq-client
        mq-client                                0              default
        mq-client-base                           0              default
        mq-client-emea                           0
        mq-client-local                          0              mq-client-base
        mq-client-us                             0

    You can see that there's one client profile for every broker group, mq-client-us and mq-client-example in our case.

4. Now we can easily connect producers and consumers to the by applying appropriate profiles to the container. For example to start producing to the **us** broker, we need to use both **mq-client-us** and **example-mq-producer**

        FuseFabric:karaf@root> container-create-child --profile mq-client-us --profile example-mq-producer root producer-us
        The following containers have been created successfully:
	    Container: producer-us.

    The similar states for other containers

        FuseFabric:karaf@root> container-create-child --profile mq-client-emea --profile example-mq-producer root producer-emea
        The following containers have been created successfully:
	    Container: producer-emea.

        FuseFabric:karaf@root> container-create-child --profile mq-client-us --profile example-mq-consumer root consumer-us
        The following containers have been created successfully:
	    Container: consumer-us.

        FuseFabric:karaf@root> container-create-child --profile mq-client-emea --profile example-mq-consumer root consumer-emea
        The following containers have been created successfully:
	    Container: consumer-emea.




### Camel clients

The **mq-fabric-camel** defines an **amq** camel component which works just like the **activemq** camel component only it depends on an ActiveMQConnectionFactory being injected from the OSGi registry (and refuses to start until one is available). So like above we can use the mq-client profiles to combine with the **mq-fabric-camel** model and the **amq* component to work with A-MQ brokers.

For example the **example-camel-mq** profile is a simple profile defining a camel route in its [camel.xml](https://github.com/jboss-fuse/fuse/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/configs/versions/1.0/profiles/example-camel-mq/camel.xml) which uses the **amq:** endpoints to work with [Apache ActiveMQ]

If you deploy it with a client profile then it will connect to that client profile's broker group. e.g.

* **example-camel-mq** and **mq-client-us** to connect camel route to the **us** broker group
* **example-camel-mq** and **mq-client-emea** to connect camel route to the **emea** broker group

To demonstrate this, we can reuse first three steps from the example above. Finally we'd start a camel demo that uses broker in **us** group with

    FuseFabric:karaf@root> container-create-child --profile mq-client-us --profile example-camel-mq root example-us
    The following containers have been created successfully:
	    Container: example-us.

In the similar fashion we can start a demo that uses *emea* group

    FuseFabric:karaf@root> container-create-child --profile mq-client-emea --profile example-camel-mq root example-emea
    The following containers have been created successfully:
	    Container: example-emea.