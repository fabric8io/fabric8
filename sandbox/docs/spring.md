## Using Spring framework with Fabric8

This chapter contains guidelines for people willing to use Fabric8 in order to provision and manage Spring-based
applications.

### Spring Dynamic Modules (deprecated)

In the older versions of the Karaf, ServiceMix and Fuse we recommended to use 
[Spring Dynamic Modules](http://docs.spring.io/osgi/docs/1.2.1/reference/html) to deploy applications based on the
Spring framework. As Spring DM project is not actively maintained anymore, we currently don't recommend to use it. 
Moreover due to the fact that there is no other alternative for Spring deployments in the OSGi environment, we don't 
recommend provisioning Spring-based applications into the OSGi anymore.

### Spring Boot container

The recommended approach for working with the Spring applications in the Fabric8 environment is to use the
[Spring Boot container](springBootContainer.md) - the flat classpath microservice provisioned and managed by the
Fabric8.

You can provision Spring Boot container either as a [managed JVM process](processContainer.md) or as a 
[Docker container](docker.md).

Spring Boot container comes with many useful utilities that makes it much easier to use Apache Camel and
ActiveMQ. Spring Boot containers can be also aware of the [Gateway](gateway.md), ZooKeeper registry and the other Fabric8 
components.

### Other flat classpath containers

The other option to work with the Spring-based applications in the Fabric8 environment is to deploy those as a part
of the WAR into Tomcat or as a module into the WildFly or Apache TomEE.
