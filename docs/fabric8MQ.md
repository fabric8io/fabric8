## Fabric8 Messaging

Fabric8 Messaging provides a scalable and elastic _Messaging as a Service_ built on top of Kubernetes.

Fabric8 Messaging comprises of the following microservices:

* **Message Broker** is an elastic pool of messaging pods based on [Apache ActiveMQ Artemis](https://activemq.apache.org/artemis/) which supports JMS 2.0 and various protocols like AMQP, OpenWire, MQTT and STOMP on a single port, 61616 so its easier to work with Kubernetes external services (e.g. OpenShift's haproxy)
* **Message Gateway** performs discovery, load balancing and sharding of message Destinations across the pool of message brokers to provide linear scalability of messaging.
* **ZooKeeper** is used by the Message Gateway so that the Message Gateway pods can coordinate to share destinations across Message Broker pods

For more background see the [Architecture](#architecture).

### Running Fabric8 Messaging

* In the [Console](console.html) click on the **Runtime** tab and select the Project you wish to run things inside
* Click on the **Run** button on the top right to run the **Messaging** application
* Now run the **Example Message Producer** and **Example Message Consumer** apps to produce and consume messages
* You could try any of the Message or ActiveMQ based [Quickstarts](quickstart.html)

### Using Fabric8 Messaging

The Message Gateway implements a service, `activemq` on port 61616 so any messaging application can just connect to `tcp://activemq:61616` and use any of the messaging protocols supported. 

#### Using JMS

If you are using JMS then if you use the [mq-client](https://github.com/fabric8io/fabric8-ipaas/tree/master/mq-client) library the **io.fabric8.mq.core.MQConnectionFactory** class will automatically default to using the `activemq` message service for scalable messaging.

#### Using Camel amq component

If you use the [camel-amq](https://github.com/fabric8io/fabric8-ipaas/tree/master/camel-amq) library and `amq:` component it will automatically default to using the `activemq` message service for scalable messaging.


#### Use a Message Gateway Sidecar

If your application wishes to avoid a network hop between your container and the Message Gateway you can just add the Message Gateway container into your Pod; then your container can connect on `tcp://localhost:61616` to perform messaging with the gateway taking care of communicating with the correct broker pods based on the destinations you use.


#### Enviornment variables

If you wish to connect to a different messaging service other than `activemq` then use the `$ACTIVEMQ_SERVICE_NAME` environment variable. 


### Fabric8 Messaging Architecture

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mq.png "Fabric8 Messaging core")

Its best explained by following what happens to messages as they flow through Fabric8 Messaging:

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mqflow.png "Fabric8 Messaging message flow")
 
 1. Protocol Conversion — by moving MQTT,STOMP,AMQP protocol conversion outside of the ActiveMQ broker reduces the amount of work the broker needs to do: As ActiveMQ isn’t well designed for modern, asynchronous tasks, the less work the broker does, the better.
 
 2. Camel Routing is built right into the router — so we can easily convert on the fly between, say Topics (Publish/Subscribe) and Queues (point-2-point).
 
 3. API Management — utlizing API Man, so you can apply security and rate limiting policies to destinations
 
 4. Multiplexer — reducing the amount of physical connections an ActiveMQ broker has, increases the throughput of the overall system.
 
 5. Destination Sharding — this is where a lot of the magic pixie dust is used, to shard connections across many different backend ActiveMQ brokers. Its also where messages and clients are moved between brokers, as brokers are spun up and down, depending on load
 
 6. Broker Control — this monitors the brokers under Fabric8 Messaging’s control and monitors load — and it decides if more ActiveMQ brokers are needed, or less — depending on load. An embedded rules engine is used to make the decisions.
  
Fabric8 Messaging utilises Fabric8 and Kubernetes and its individual components are all independently scalable:

![alt text](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabric8mqscalable.png "Fabric8 Messaging scales")

