## Fabric8 Quickstarts

Fabric8 ships with a set of quickstarts which shows you how to build a
docker image and run it on Kubernetes / OpenShift. All quickstart can
either run directly by checking it out from the
[fabric8-quickstarts GitHub organization](https://github.com/fabric8-quickstarts)
or run as an archetype.


First time users of fabric8 may enjoy a [walk through](walkthrough.md) a simple quickstart to show you step by step how to work with the quickstarts. A [video of the walkthrough](https://vimeo.com/142658441) is also available.


The following quickstarts are available:

* [CDI](https://github.com/fabric8-quickstarts?q=cdi)
  quickstarts using standalone Java container with CDI injected
  components
  * **camel** shows how to work with Camel in the Java container using CDI.
  * **camel-mq** shows how to call an ActiveMQ broker Kubernetes
    service from a Camel CDI application. 
  * **camel-http** - shows how to call a Kubernetes service with HTTP
    from a Camel CDI application. 
  * **cxf** - shows how to work with CXF in the Java Container using
    CDI to configure CXF REST services. 
* [Java](https://github.com/fabric8-quickstarts?q=java)
  quickstarts using standalone plain Java container 
  * **camel-spring** demonstrates how to run a Spring based Camel
    application as a standalone Java container. The Camel route is
    defined in a Spring XML file. 
  * **simple-fatjar** this example shows how to start the Java
    Container using your custom main class as a FAT jar. 
  * **simple-mainclass** this example shows how to start the Java
    Container using your custom main class as a main class.
* [Karaf](https://github.com/fabric8-quickstarts?q=karaf) 
  quickstarts using Apache Karaf containers.
  * **camel-amq** demonstrates using Apache Camel to send and recieve
    messages to an Apache ActiveMQ message broker, using the Camel
    [amq](https://github.com/fabric8io/fabric8-ipaas/tree/master/camel-amq)
    component. 
  * **camel-log** is a beginner example using Apache Camel that logs a
    message every 5th second. 
  * **camel-rest-sql** demonstrates how to use SQL via JDBC along with
    Camel's REST DSL to expose a RESTful API. 
  * **cxf-rest** is a set of web service and REST examples using
    Apache CXF. 
* [Spring Boot](https://github.com/fabric8-quickstarts?q=spring-boot) 
  quickstarts 
  * **camel** demonstrates how you can use Apache Camel with Spring Boot.
  * **webmvc** demonstrates how you can use Spring MVC with Spring Boot.
* [War](https://github.com/fabric8-quickstarts?q=war%20OR%20wildfly)
  quickstarts are using Java Servlet containers, supporting WAR deployments.
  * **camel-servlet** demonstrates how you can use Servlet to expose a
    http service in a Camel route, and run that in a servlet container
    such as Apache Tomcat.
  * **cxf-cdi-servlet** demonstrates how to create a RESTful (JAX-RS)
    web service using Apache CXF and expose it using CDI running in
    servlet container as a war
  * **wildfly** demonstrates how to deploy a simple Application as war
    on a Wildfly instance

You can use this quickstarts either as an [archetype](archetypes.md)
or directly by checking it out from the
[https://github.com/fabric8-quickstarts](https://github.com/fabric8-quickstarts)
GitHub organization.

Detailed instruction for running the quickstarts can be found in an
each section ["Running Quickstarts"](running.md)



