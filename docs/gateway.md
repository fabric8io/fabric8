## Gateway

The Gateway provides a TCP and HTTP/HTTPS gateway for discovery, load balancing and failover of services running within a Fabric8. This allows simple HTTP URLs to be used to access any web application or web service running withing a Fabric; or for messaging clients with A-MQ using any protocol (OpenWire, STOMP, MQTT, AMQP or WebSockets) they can discover and connect to the right broker letting the gateway deal connection management and proxy requests to where the services are actually running.

### Deployment options

There are 2 main deployment strategies

* run the gateway on each machine which needs to discover services; then communicate with it via localhost. You then don't need to hard code any host names in your messaging or web clients and you get nice fast networking on localhost.
* run the gateway on one or more known hosts using DNS or VIP load balancing of host names to machines; then you can use a fixed host name for all your services

### How the Gateway works

The gateway watches the ZooKeeper registry for all web applications, web services, servlets and message brokers; then uses the mapping rules to figure out how to expose those services via the TCP or HTTP gateways.

The ZooKeeper registry is automatically populated by fabric8 when you deploy WARs or CXF based web services.

### Running the Gateway

From the CLI or Fuse Management Console just run an instance of the **gateway-mq** profile for messaging or **gateway--http** for HTTP based gateway on a machine you wish to use as the gateway (e.g. if using Red Hat clustering and VIPs on 2 boxes), or on the same machine as you wish to connect to services from non-Fabric Java clients (e.g. from a C based AMQP client).

### Configuring the Gateway

To configure the gateway, navigate to the profile page then click on the **Configuration** tab, then select either the **Fabric8 HTTP Gateway** or the **Fabric8 MQ Gateway** to configure its settings.

### HTTP Mapping rules

When using the HTTP gateway, its common to wish to map different versions of web applications or web services to different URI paths on the gateway. You can perform very flexible mappings using [URI templates](http://en.wikipedia.org/wiki/URL_Template).

The out of the box defaults are to expose all web applications and web services at the context path that they are running in the target server. For example if you use the **example-quickstarts-rest** profile, then that uses a URI like: **/cxf/crm/customerservice/customers/123** on whatever host/port its deployed on; so by default it is visible on the gateway at [http://localhost:9000/cxf/crm/customerservice/customers/123](http://localhost:9000/cxf/crm/customerservice/customers/123)

For this the URI template is:

    {contextPath}/

which means take the context path (in the above case "/cxf/crm" and append "/" to it, making "/cxf/crm/" and then any request within that path is then passed to an instance of the cxf crm service.

#### Choosing different parts of the ZooKeeper registry to map

The mapping rules for MQ and HTTP monitor regions of the ZooKeeper registry; you give a path which then all descendants are considered to be suitable services to gateway to.

In a messaging world, you could then provide a gateway to all message brokers worldwide; or could provide continent, country or region specific gateways by just specifying different ZooKeeper paths for each gateway configuration. For regional messaging clusters we use different folders in ZooKeeper for different geographic broker clusters.

With HTTP then REST APIs, SOAP Web Services, servlets and web applications all live in different parts of the ZooKeeper registry. You can browse the contents of the registry with the **Registry** tab in the **Runtime** section of the console.

Here are the common ZooKeeper paths:

<table class="table table-striped">
<tr>
<th>ZooKeeper Path</th>
<th>Description</th>
</tr>
<tr>
<td>/fabric/registry/clusters/apis/rest</td>
<td>REST based web services</td>
</tr>
<tr>
<td>/fabric/registry/clusters/apis/ws</td>
<td>SOAP based web services</td>
</tr>
<tr>
<td>/fabric/registry/clusters/servlets</td>
<td>Servlets (registered usually individually via the OSGI APIs)</td>
</tr>
<tr>
<td>/fabric/registry/clusters/webapps</td>
<td>Web Applications (i.e. WARs)</td>
</tr>
</table>

#### Segregating URI paths

You may wish to segregate, say, servlets, web services or web applications into different URI spaces.

For example you may want all web services to be within **/api/** and apps to be in **/app/**. To do this just update the URI templates as follows:

For the web services mapping rule:

    ZooKeeperPath: /fabric/registry/clusters/apis
    URI template: /api{contextPath}/

For the web apps mapping rule:

    ZooKeeperPath: /fabric/registry/clusters/webapps
    URI template: /app{contextPath}/

If you want to split RESTful APIs and SOAP web services into different URI paths then replace the former mapping rule with these two

    ZooKeeperPath: /fabric/registry/clusters/apis/rest
    URI template: /rest{contextPath}/

    ZooKeeperPath: /fabric/registry/clusters/apis/ws
    URI template: /ws{contextPath}/

### Versioning: Explict URIs

You may wish to expose all available versions of each web service and web application at a different URI. e.g. if you change your URI template to:

    /version/{version}{contextPath}/

Then if you have 1.0 and 1.1 versions of a profile with web services or web apps inside, you can access them using version specific URIs. For example if you are running some version 1.0 and version 1.1 implementations of the **example-quickstarts-rest** profile then you can access either one via these URIs

* version 1.0 via: [http://localhost:9000/version/1.0/cxf/crm/customerservice/customers/123](http://localhost:9000/version/1.0/cxf/crm/customerservice/customers/123)
* version 1.1 via: [http://localhost:9000/version/1.1/cxf/crm/customerservice/customers/123](http://localhost:9000/version/1.1/cxf/crm/customerservice/customers/123)

Then both versions are available to the gateway - provided you include the version information in the URI

### Versioning: Rolling Upgrades

Another approach to dealing with versions of web services and web applications is to only expose a single version of each web service or web application at a time in a single gateway. This is the default out of the box configuration.

So if you deploy a 1.0 version of the **gateway-http** profile and run a few services, then you'll see all 1.0 versions of them. Run some 1.1 versions of the services and the gateway won't see them. Then if you do a [rolling upgrade](rollingUpgrade.md) of your gateway to 1.1 it will then switch to only showing the 1.1 versions of the services.

If you want to be completely specific on a version you can specify the exact _profile_ version on the mapping configuration screen.

The other approach when using web applications is you could specify the maven coordinates and maven version of a web application in the ZooKeeper path.

### URI template expressions

The following table outlines the available variables you can use in a URI template expression


<table class="table table-striped">
<tr>
<th>Expression</th>
<th>Description</th>
</tr>
<tr>
<td>{bundleName}</td>
<td>The bundle name which registers the web service, servlet or application. This is an optional value (e.g. its not currently supported for web services) but works for web apps and servlets in OSGi.</td>
</tr>
<tr>
<td>{bundleVersion}</td>
<td>The bundle version which registers the web service, servlet or application. This is an optional value (e.g. its not currently supported for web services) but works for web apps and servlets in OSGi.</td>
</tr>
<tr>
<td>{container}</td>
<td>The container ID of the web service or web application</td>
</tr>
<tr>
<tr>
<td>{contextPath}</td>
<td>The context path (the part of the URL after the host and port) of the web service or web application implementation.</td>
</tr>
<tr>
<td>{servicePath}</td>
<td>The relative path within ZooKeeper that a service is registered; this usually is made up of, for web services as the service name and version. For web applications its often the maven coordinates</td>
</tr>
<tr>
<td>{version}</td>
<td>The profile version of the web service or web application</td>
</tr>
</table>

### Viewing all the active HTTP URIs

Once you've run a few web services and web applications and you are runnning the gateway you may be wondering what URIs are available. Assuming you're on a machine with the gateway, just browse the URL [http://localhost:9000/]([http://localhost:9000/) and you should see the JSON of all the URI prefixes and the underlying servers they are bound to.

## Haproxy Gateway

If you are using [haproxy](http://haproxy.1wt.eu/) as your HTTP load balancer you can use the **gateway-haproxy** profile to automatically generate your haproxy configuration file in real time whenever web services, web applications or servlets are deployed or undeployed.

The haproxy gateway uses the same URI template mapping rules above to know how to map front end URI requests to back ends and server instances; so it can generate the detail of the haproxy configuration file.

### Configuring the haproxy gateway

To get the haproxy gateway working you need to configure it so that:

* you have specified the generated configuration file's output file name (which should be a place that your haproxy installation will load from)
* optionally specify a command and directory for the command so that haproxy can be gracefully reloaded. This may be something like:

```
sudo haproxy -p /var/run/haproxy.pid -sf $(cat /var/run/haproxy.pid)
```

Then the haproxy configuration file will get regenerated whenever web applications, web services or servlets are added or removed and haproxy reloaded and the command will tell haproxy to reload its configuration.

If you wish to change the actual haproxy configuration, please edit the MVEL template inside the profile which is used to generate the actual haproxy configuration. **Note** changes to the generated configuration file will get overwritten next time a back end service implementation comes or goes (as the file gets regenerated).

