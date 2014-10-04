## Gateway Model

This project defines a data model which can be created via a Java DSL, loaded or saved as XML via JAXB or JSON via Jackson loaded from fabric8 via profiles and OSGi MetaType properties files (for simple UI tooling).

The idea is we can define in a technology agnostic way HTTP mapping rules for exposing RESTful endpoints to customer facing URIs, maybe outside of the corporate firewall, such that we can implement an API Gateway, BaaS / MBaaS or Mobile Gateway.

### Implementing the Model

Various technologies could be used to implement the mapping rules. We have currently these implementations

* **gateway-model** the technology agnostic model for defining gateways (Java DSL and DTOS)
* **gateway-servlet** is a servlet filter based implementation of the gateway
* **gateway-servlet-example** an example WAR which uses the [Java DSL to define gateways in a single ExampleServlet](https://github.com/fabric8io/fabric8/blob/master/gateway/gateway-servlet-example/src/main/java/io/fabric8/gateway/example/ExampleServlet.java#L38)
* **gateway-fabric** a fabric8 based implementation using vertx which auto-proxies RESTful endpoints, web applications and web services in fabric8
* **gateway-fabric-haproxy** a fabric8 based implementation which works like gateway-fabric but instead of implementing the proxy in vertx it reuses existing HTTP proxy tools like haproxy to implement the proxy; and auto-generates the configuration of haproxy baesd on the metadata from fabric8

### Trying it out

The easiest way to try it out is to clone the repository and do a build with [Maven](http://maven.apache.org/]:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8/gateway
    mvn install
    cd gateway-servlet-example
    mvn jetty:run

Now if you open a web page such as [http://localhost:8080/gateway/search/cheese](http://localhost:8080/gateway/search/cheese) the mapping rule defined in the [ExampleServlet](https://github.com/fabric8io/fabric8/blob/micro-service/gateway/gateway-servlet-example/src/main/java/io/fabric8/gateway/example/ExampleServlet.java#L38) should kick in and, in this case, the page should proxy to a google search for cheese


