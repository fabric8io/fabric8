Provides a single port TCP [gateway](http://fabric8.io/gitbook/gateway.html) between http and messaging clients and services running in the Fabric with automatic load balancing and failover.

The gateway handles detecting the client protocol.  If it's an HTTP client, the connection will be routed to the HTTP gateway.  If it's a messaging client, it will use any supplied host information provided by the client to route the connection to a broker Fabric group.
