## Gateway

The Gateway provides a TCP and HTTP/HTTPS gateway for discovery, load balancing and failover of services running within a Fabric8. This allows simple HTTP URLs to be used to access any web application or web service running withing a Fabric; or for messaging clients with A-MQ using any protocol (OpenWire, STOMP, MQTT, AMQP or WebSockets) they can discover and connect tothe right broker letting the gateway deal connection management and proxy requests to where the services are actually running.

### Deployment options

There are 2 main deployment strategies

* run the gateway on each machine which needs to discover services; then communicate with it via localhost. You then don't need to hard code any host names in your messaging or web clients and you get nice fast networking on localhost.
* run the gateway on one or more known hosts using DNS or VIP load balancing of host names to machines; then you can use a fixed host name for all your services

### Running the Gateway

From the CLI or Fuse Management Console just run an instance of the **gateway-default** profile on a machine you wish to use as the gateway (e.g. if using Red Hat clustering and VIPs on 2 boxes), or on the same machine as you wish to connect to services from non-Fabric Java clients (e.g. from a C based AMQP client).

### Configuring the Gateway

The gateway is run via a Profile in Fabric8. The configuration file is called **io.fabric8.gateway.json**.

Here's the [default configuration](https://github.com/jboss-fuse/fuse/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/configs/versions/1.0/profiles/gateway-default/io.fabric8.gateway.json) that comes in the **gateway-default**. So you can edit the JSON to change which ports are proxied and to configure which clusters are discovered in ZooKeeper.
