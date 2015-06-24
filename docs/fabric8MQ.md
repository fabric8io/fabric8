## Fabric8 MQ

Fabric8MQ isn’t a new message broker, its a smart messaging proxy that utilises Fabric8/Kubernetes to create Messaging as a Service out of automatically sharded ActiveMQ brokers. It sits on top of a Vert.x core to provide highly scalable asynchronous connection handling:

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mq.png "Fabric8MQ core")

Its best explained by following what happens to messages as they flow through Fabric8MQ:

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mqflow.png "Fabric8MQ message flow")

1. Protocol Conversion — by moving MQTT,STOMP,AMQP protocol conversion outside of the ActiveMQ broker reduces the amount of work the broker needs to do: As ActiveMQ isn’t well designed for modern, asynchronous tasks, the less work the broker does, the better.

2. Camel Routing is built right into the router — so we can easily convert on the fly between, say Topics (Publish/Subscribe) and Queues (point-2-point).

3. API Management — utlizing API Man, so you can apply security and rate limiting policies to destinations

4. Multiplexer — reducing the amount of physical connections an ActiveMQ broker has, increases the throughput of the overall system.

5. Destination Sharding — this is where a lot of the magic pixie dust is used, to shard connections across many different backend ActiveMQ brokers. Its also where messages and clients are moved between brokers, as brokers are spun up and down, depending on load

6. Broker Control — this monitors the brokers under Fabric8MQ’s control and monitors load — and it decides if more ActiveMQ brokers are needed, or less — depending on load. An embedded rules engine is used to make the decisions.


Fabric8MQ utilises Fabric8 and Kubernetes and its individual components are all independently scalable:

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mqscalable.png "Fabric8MQ scales")



The Fabric8 MQ [App](apps.html) creates a [service](services.html) using a [replication controller](replicationControllers.html) for using [Apache ActiveMQ](http://activemq.apache.org/) effectively within Fabric8 or Kubernetes.

### Running Fabric8 MQ

* In the [Console](console.html) click on the **Library** tab and then navigate into **apps** and then click on **Fabric8 MQ**
* Click on the **Run** button on the top right to run the **Fabric8 MQ** service
* Now run any ActiveMQ based [Quickstart](quickstart.html) or you could try running the **Fabric8 MQ Producer** and **Fabric8 MQ Consumer** apps in the **apps** folder of the **Library**
* Once the MQ broker is up, if you navigate to the **Pods** tab and click the connect icon you should be able to look inside the broker and see its Queues / Topics and metrics.

### Connecting to Fabric8 MQ

There are various ways to discover and connect to Fabric8 MQ depending on your requirements:

#### MQConnectionFactory

The **io.fabric8.mq.core.MQConnectionFactory** is a JMS ConnectionFactory which connects to ActiveMQ using the [Kubernetes Service](services.html) discovery mechanism described below.

Just create the MQConnectionFactory and it will use the environment variables of the current process to discover the ActiveMQ brokers.

#### Camel amq component

The Camel **amq:** component uses the [Kubernetes Service](services.html) discovery mechanism described below to discover and connect to the ActiveMQ brokers. So you can just use the endpoint directly; no configuration is required.

e.g. just use the camel endpoint **"amq:Cheese"** to access the Cheese queue in ActiveMQ; no configuration required!

The Camel **amq:** is used in the example **Fabric8 MQ Producer** and **Fabric8 MQ Consumer** apps mentioned above.

#### Environment variables

* You can use the usual [Kubernetes Service](services.html) mechanism to connect to the Fabric8 MQ service; namely using the environment variables:
  * **FABRIC8MQ_SERVICE_HOST** as the host to connect to ActiveMQ
  * **FABRIC8MQ_SERVICE_PORT** as the port to connect to ActiveMQ

Then your application does not need any special Java code or discovery logic; it connects to a fixed host and port which is constant for a given environment. Then [Kubernetes Service](services.html) takes care of the load balancing and discovery; out of your process (but on the same host).

