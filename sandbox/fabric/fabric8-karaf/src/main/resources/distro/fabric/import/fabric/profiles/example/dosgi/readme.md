## Introduction

This example demonstrates three features:

* Configuration of a Zookeeper registry on a Karaf instance and deployment of local containers
* Provisioning of artifacts (repositories, features, bundles, configurations) based on profiles usage
* Implementation of an example using a service distributed based on OSGI spec - Remote Services (see Chapter 13 of document www.osgi.org/download/r4v42/r4.enterprise.pdf )

## Description

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

![fabric-osgi.png](https://raw.githubusercontent.com/fabric8io/fabric8/master/fabric/fabric-examples/fabric-camel-dosgi/fabric-dosgi.png)

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


## Compiling

    cd fabric-examples/fabric-camel-dosgi
    mvn clean install

## How to try this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a container and assign it the example-dosgi-camel.provider profile

    fabric:container-create-child --profile example-dosgi-camel.provider --parent root dosgi-provider

1. Create a container and assign it the example-dosgi-camel.consumer profile

    fabric:container-create-child --profile example-dosgi-camel.consumer --parent root dosgi-camel

1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

You should see similar log statements as above:

```
   fabric-client | 71 - org.apache.camel.camel-core - 2.13.1 | >>> Response from : Message from distributed service to : Fabric Container
   fabric-client | 71 - org.apache.camel.camel-core - 2.13.1 | >>> Response from : Message from distributed service to : Fabric Container
   fabric-client | 71 - org.apache.camel.camel-core - 2.13.1 | >>> Response from : Message from distributed service to : Fabric Container
```

## Running from web console

Install the consumer and provider profile into separate containers. Connect to the consumer and see the log tab to see what happens.

