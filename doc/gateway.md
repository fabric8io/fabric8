## Fuse Gateway

The Fuse Gateway provides a HTTP/HTTPS/TCP gateway for discovery and load balancing of services running within a Fuse Fabric. This allows simple HTTP URLs to be used to access any web application or web service running withing a Fabric; or in the case of A-MQ; any messaging client for ActiveMQ, STOMP, MQTT, AMQP or WebSockets to connect to the relevant protocol port on localhost; and the gateway will use the Fuse Fabric registry to discover the brokers and deal with connection management and proxy requests to where the services are actually running.

### Running the Gateway

From the CLI or Fuse Management Console just run an instance of the **gateway-default** profile on a machine you wish to use as the gateway (e.g. if using Red Hat clustering and VIPs on 2 boxes), or on the same machine as you wish to connect to services from non-Fabric Java clients (e.g. from a C based AMQP client).
