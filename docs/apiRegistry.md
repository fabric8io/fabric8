## API Registry

The API Registry implements a highly available RESTful [service](services.html) using a [replication controller](replicationControllers.html) to provide a global view of all of your RESTful and web service APIs.

It currently discovers all [Swagger](http://swagger.io/), [WADL](http://www.w3.org/Submission/wadl/) and [WSDL](http://www.w3.org/TR/wsdl) contracts used by any [Apache CXF](http://cxf.apache.org/) based REST or web services or any [Camel REST DSL routes](http://camel.apache.org/rest-dsl.html). Any JVM is supported; so your CXF and Camel can be running inside Karaf, Tomcat, Wildfly or a vanilla JVM or Spring Boot etc.

### Requirements

For the API Registry to be able to discover the API contracts and endpoints then JMX must be enabled in your JVM (which it tends to be by default), then JMX needs to be enabled in CXF and Camel.

In addition the JVM needs to expose its [Jolokia port](http://jolokia.org/) which it does by default in the base container images used in in the various [quickstarts](quickstarts.html).

For CXF you need to ensure that JMX is properly enabled and that the **fabric-cxf** library is included in your CXF application which adds the necessary JMX operations so that we can remotely discover the correct endpoints inside the JVM.

### Using the API Registry

* In the [Console](console.html) click on the **Library** tab and then navigate into **apps** and then click on **API Registry**
* Click on the **Run** button on the top right to run the **API Registry** service
* Now run any [Quickstart](quickstart.html) which uses CXF REST or web services or the **Camel Rest SQL** quickstart.
* Wait a minute or so for everything to be up and running
* You should see a new **APIs** tab in the [Console](console.html) click on it
* You should now see the **API Console** which lets you view, search and navigate to all the APIs defined in your environment for [CXF](http://cxf.apache.org/) or [Camel REST DSL routes](http://camel.apache.org/rest-dsl.html).
