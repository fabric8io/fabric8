## Gateway

This folder contains the various [gateway](http://fabric8.io/gitbook/gateway.html) profiles provided by Fabric.

The Gateway provides a TCP and HTTP/HTTPS gateway for discovery, load balancing and failover of services running within a Fabric8. This allows simple HTTP URLs to be used to access any web application or web service running withing a Fabric; or for messaging clients with A-MQ using any protocol (OpenWire, STOMP, MQTT, AMQP or WebSockets) they can discover and connect to the right broker letting the gateway deal connection management and proxy requests to where the services are actually running.

Each of the profile has more information, as well in the Fabric [gateway](http://fabric8.io/gitbook/gateway.html) user guide.

* [http](/fabric/profiles/gateway/http.profile) gateway for HTTP traffic
* [mq](/fabric/profiles/gateway/mq.profile) gateway for messaging using A-MQ
