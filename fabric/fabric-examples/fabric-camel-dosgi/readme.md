# INTRODUCTION

This example demonstrates three features proposed by Fuse Fabric project :

* Configuration of a Zookeeper registry on a Karaf instance and deployment of local containers
* Provisioning of artifacts (repositories, features, bundles, configurations) based on profiles usage
* Implementation of an example using a service distributed based on OSGI spec - Remote Services (see Chapter 13 of document www.osgi.org/download/r4v42/r4.enterprise.pdf )

# Explanation

The service that we will distribute is a java POJO created using an interface

    public interface Service {

        public String messageFrom(String input);

    }

and implemented here

    public class ServiceImpl implements Service {

        @Override
        public String messageFrom(String input) {
            return "Message from distributed service to : " + input;
        }
    }

To register this service (= Interfaces) into the OSGI registry, we use the following Blueprint syntax

    <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0">

        <bean id="myService" class="io.fabric8.example.dosgi.impl.ServiceImpl"/>

        <service ref="myService" auto-export="interfaces">
            <service-properties>
                <entry key="service.exported.interfaces" value="*"/>
            </service-properties>
        </service>

    </blueprint>

During this process, Fabric will publish information in the Zookeeper registry. That will allow another Fabric container to discover them at runtime

![fabric-osgi.png](https://github.com/fusesource/fuse/raw/master/fabric/fabric-examples/fabric-camel-dosgi/fabric-dosgi.png)

In another bundle, we will create a Camel route where we will refer to this service using as key the name of the interface that we will lookup into
the Zookeeper registry to find it and get locally a proxy object !

<reference id="myService" interface="io.fabric8.example.dosgi.Service" availability="optional"/>

<camelContext id="camel" trace="false" xmlns="http://camel.apache.org/schema/blueprint">

  <route id="fabric-client">
    <from uri="timer://foo?fixedRate=true&amp;period=10000"/>
    <setBody>
        <constant>Karaf Zookeeper server</constant>
    </setBody>
    <bean ref="myService" method="messageFrom"/>
    <log message=">>> Response from : ${body}"/>
  </route>

</camelContext>


# COMPILING

    cd fabric-examples/fabric-camel-dosgi
    mvn clean install

# RUNNING

1) Before you run Karaf you might like to set these environment variables...

    export JAVA_PERM_MEM=64m
    export JAVA_MAX_PERM_MEM=512m

2) Download and install a fresh distribution of Fuse ESB enterprise or Fabric 7.x (http://fuse.fusesource.org/fabric/download.html)

And run the following command in the console

3) Initialize a local Fabric

    fabric:create --clean root


4) Create a container and assign it the example-dosgi-camel.provider profile

    fabric:container-create --profile example-dosgi-camel.provider --parent root dosgi-provider

5) Create a container and assign it the example-dosgi-camel.consumer profile

    fabric:container-create --profile example-dosgi-camel.consumer --parent root dosgi-camel

6) Check that the consumer routes and see the route info of consumer

    shell:watch fabric:container-connect dosgi-camel camel:route-info fabric-client

   The command above will automatically refresh the output every second.

   or Connect to the dosgi-camel container and verify that Camel logs this info

   fabric-client | 71 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from : Message from distributed service to : Fuse Fabric Container
   fabric-client | 71 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from : Message from distributed service to : Fuse Fabric Container
   fabric-client | 71 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from : Message from distributed service to : Fuse Fabric Container

Enjoy!
