# Camel Cluster Example

This is a client/server example where the client and server communications using HTTP. The server exposes a HTTP service using a Camel route with the Jetty component. The HTTP service is exposes using the [Fabric endpoint](http://fabric8.io/gitbook/camelEndpointFabric.html).

The client is also a Camel route that discover the HTTP service from the Fabric registry, also using the [Fabric endpoint](http://fabric8.io/gitbook/camelEndpointFabric.html). 

The fabric endpoint provides a way to reuse Fabric's discovery mechanism to expose physical socket & HTTP endpoints into the runtime registry using a logical name so that clients can use the existing Camel Load Balancer. 

This is illustrated in the figure below:

![Fabric Camel endpoint](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/fabic-camel-endpoint.png)

## To run this example

To run this example you need to create and deploy each profile in a new container.

Then you can connect to the server and/or client container and open the **Log** tab to see activity.

### Creating multiple servers

If you create a 2nd server, then the client will load balance (using random) between the active servers, as shown from the following log output from the client. Notce that the log outputs `myserver` and `myserver2` which is the container names of our servers.

```
2014-08-29 11:14:09,792 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver
2014-08-29 11:14:10,790 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver
2014-08-29 11:14:11,790 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver
2014-08-29 11:14:12,790 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver
2014-08-29 11:14:13,793 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver2
2014-08-29 11:14:14,790 | INFO  | #0 - timer://foo | fabric-client                    | ?                                   ? | 96 - org.apache.camel.camel-core - 2.13.2 | >>> Response from Fabric Container : myserver2
```

If a server is stopped, deleted or crashes, then the client will be able to automatic continue and use the currently active servers. 

### Viewing the fabric registry

When the example is running, you can use the web console to view the Fabric registry and see the information that the client discovers.

