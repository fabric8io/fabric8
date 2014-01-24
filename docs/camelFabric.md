## Camel Fabric

When using [ActiveMQ](http://activemq.apache.org) or JMS endpoints in Camel you are already fabric ready since message brokers provide true location independence (as well as time independence too, the consumer of a message does not even have to be running at the time you send a message).

However if you are using socket or HTTP endpoints this is not the case; each client wishing to invoke your service needs to know all the available network addresses of each implementation (e.g. a list of protocols, host name and ports).

This is where the **Camel Fabric** comes in; it provides a way to reuse Fabric's discovery mechanism to expose physical socket & HTTP endpoints into the runtime registry using a logical name so that clients can use the existing [Camel Load Balancer](http://camel.apache.org/load-balancer.html).

Currently Camel Fabric works using the fabric Camel component.

### Exposing a Camel endpoint into the fabric

    from("fabric:myName:jetty:http://0.0.0.0:8181").to("bean:foo")

Here we just prefix any socket or HTTP based endpoint with **fabric:myName** where the myName is the logical name of the service you wish to use. This prefix is then used to expose your endpoint URI into the Runtime Registry

### Invoking a Camel fabric endpoint

    from("seda:foo").to("fabric:myName")

In the above route we send to the endpoint fabric:myName which at runtime will use the myName entry in the Camel Fabric to discover the currently available physical endpoints in the fabric for this name; then load balance across them.

