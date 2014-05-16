## Gateway Model

This project defines a data model which can be loaded or saved as XML via JAXB, created via a Java DSL or loaded from fabric8 via profiles and OSGi MetaType properties files (for simple UI tooling).

The idea is we can define in a technology agnostic way HTTP mapping rules for exposing RESTful endpoints to customer facing URIs such that we can implement an API Gateway, BaaS / MBaaS or Mobile Gateway.

### Implementing the Model

Various technologies could be used to implement the mapping rules. We have currently these implementations

* **gateway-servlet** is a servlet filter based implementation of the gateway
* **gateway-fabric** a fabric8 based implementation using vertx which auto-proxies RESTful endpoints, web applications and web services in fabric8
* **gateway-fabric-haproxy** a fabric8 based implementation which works like gateway-fabric but instead of implementing the proxy in vertx it reuses existing HTTP proxy tools like haproxy to implement the proxy; and auto-generates the configuration of haproxy baesd on the metadata from fabric8