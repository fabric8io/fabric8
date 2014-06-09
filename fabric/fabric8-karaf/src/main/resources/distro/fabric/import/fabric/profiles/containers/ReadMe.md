## Containers

This folder containers various profiles used to run Java artifacts in different kinds of containers using Fabric.

### Java Containers

The following containers support the [Java Container](http://fabric8.io/gitbook/javaContainer.html) and [Micro Services](http://fabric8.io/gitbook/microServices.html). To use a Java Container you typically just need to [use a maven build and deploy your code into the fabric](http://fabric8.io//gitbook/developer.html) using the [mvn fabric8:deploy](http://fabric8.io//gitbook/mavenPlugin.html) goal and ensure the profile you pick has defined the Java main() function.

* [java](/fabric/profiles/containers/java) is the base profile from which all [Java Container](http://fabric8.io/gitbook/javaContainer.html) and [Micro Services](http://fabric8.io/gitbook/microServices.html) profiles are derived; can't be used by itself as there's no main () defined
* [java camel spring](/fabric/profiles/containers/java.camel.spring) for [Apache Camel](http://camel.apache.org/) and [Spring](http://spring.io/) applications
* [java pojosr](/fabric/profiles/containers/java.pojosr) for deploying OSGi / blueprint applications with PojoSR in a simple flat class loader
* [java spring boot](/fabric/profiles/containers/java.springboot) for using [Spring Boot](http://projects.spring.io/spring-boot/) based [Micro Services](http://fabric8.io/gitbook/microServices.html)

In addition the [debug](/fabric/profiles/containers/debug) enables remote debugging on the process in your IDE via JPDA. To find the debug port use URLs tab in the Container page (or the container-info command in the shell).

### Servlet and JEE Containers

These containers use the underlying [Process Manager](http://fabric8.io/gitbook/processManager.html) to install a distribution of an application server, allocate it unique ports (so you can run multiple instances on a machine) and install any WARs and shared JARS in to the deploy directory.

Most of these containers have limited management done externally from the process (via injecting environment variables or files). However some of them include an embedded Fabric8 Agent which offers richer, finer grained integration and management:

* [jetty](/fabric/profiles/containers/jetty) for deploying WARs and shared JARs into [Jetty](http://eclipse.org/jetty/)
* [tomcat](/fabric/profiles/containers/tomcat) for deploying WARs and shared JARs into [Apache Tomcat](http://tomcat.apache.org/)
* [tomcat.fabric8](/fabric/profiles/containers/tomcat.fabric8) for deploying WARs and shared JARs into [Apache Tomcat](http://tomcat.apache.org/) which include an embedded Fabric8 Agent
* [tomee](/fabric/profiles/containers/tomee) for deploying WARs and shared JARs into [Apache TomEE](http://tomee.apache.org/)
* [tomee.fabric8](/fabric/profiles/containers/tomee.fabric8) for deploying WARs and shared JARs into [Apache TomEE](http://tomee.apache.org/) which include an embedded Fabric8 Agent

### Services

These containers support various application services such as databases etc

* [cassandra](/fabric/profiles/containers/services/cassandra) installs an [Apache Cassandra](http://cassandra.apache.org/) node
* [cassandra.local](/fabric/profiles/containers/services/cassandra.local) installs a local cluster [Apache Cassandra](http://cassandra.apache.org/) node intended for development

