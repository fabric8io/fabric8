## Fabric8 MQ

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

