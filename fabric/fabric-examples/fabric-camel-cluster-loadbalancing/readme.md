# ﻿INTRODUCTION

This demo demonstrates the Fuse Technology called "Cloud Integration" where the Camel Routes exposing or consuming
a Service are deployed within Fabric containers running locally. The service is not published using CXF - WebService
framework but using Jetty as Web Server.
Remark : The container instead of being created locally could be on another machine (ssh) or in the cloud.

The Apache Camel routes exposing a service uses the Jetty component and the Fabric endpoint.
=======
﻿﻿INTRODUCTION
============

This example demonstrates the provisioning feature of fuseSource Fabric based on profiles
defined in zookeeper configuration registry and how we can use the fabric-camel component in
a cluster environment.  We will deploy a fabric camel route on Fuse ESB 7 instance

    <route id="fabric-slave">
      <from uri="timer://foo?fixedRate=true&amp;period=10000"/>
      <setBody>
          <simple>Hello from Zookeeper server</simple>
      </setBody>
      <to uri="fabric-camel:local"/>
      <log message=">>> ${body} : ${header.karaf.name}"/>
    </route>

and sends messages to a fabric called cheese while two other fabric camel routes deployed on zookeeper
containers and exposing HTTP Servers will be able to answer to the client.
>>>>>>> 7.1.x.fuse-stable

     <route id="fabric-server">
        <from uri="fabric-camel:local:jetty:http://0.0.0.0:{{portNumber}}/fabric"/>
        <log message="Request received : ${body}"/>
        <setHeader headerName="karaf.name">
            <simple>${sys.karaf.name}</simple>
        </setHeader>
        <transform>
            <simple>Response from Zookeeper agent</simple>
        </transform>
     </route>

The fabric endpoint allows to associate the jetty component to a logical group name. In our example, this group is called "local".
When the Fabric agent will discover that a fabric endpoint must be published, then it will call the zookeeper registry and add an entry containing
the location of the endpoint. The result hereafter shows you what Zookeeper has finally registered for 2 different endpoints under "local".

    registry/camel/endpoints/local/00000000000 = jetty:http://0.0.0.0:9191/fabric
    registry/camel/endpoints/local/00000000001 = jetty:http://0.0.0.0:9090/fabric


The camel producer (= client), instead of calling directly the camel endpoint as usal, will call a Fabric endpoint using as key
the group name "local". Then a lookup occurs in the Zookeeper registry to question it and find which endpoints have been registered.
The Camel component will get a list of endpoints that it will use randomly to publish every 2s a message
"Hello from Fabric Client to group Local"

    <route id="fabric-slave">
      <from uri="timer://foo?fixedRate=true&amp;period=2000"/>
      <setBody>
          <simple>Hello from Fabric Client to group "Local"</simple>
      </setBody>
      <to uri="fabric-camel:local"/>
      <log message=">>> ${body} : ${header.karaf.name}"/>
    </route>

Moreover, the component will load balance messages to the list of endpoints published

![fabric-camel.png](https://github.com/fusesource/fuse/raw/master/fabric/fabric-examples/fabric-camel-cluster-loadbalancing/fabric-camel.png)

# COMPILING

    cd fabric-examples/fabric-camel-cluster-loadbalancing
    mvn clean install

# RUNNING ON FUSE ESB 7

The project will deploy fabric camel routes on Fuse ESB 7 instance but you can also install a Fabric distro

1) Before you run Fuse ESB 7 you might like to set these environment variables...

    export JAVA_PERM_MEM=64m
    export JAVA_MAX_PERM_MEM=512m

In an install of Fuse ESB 7 (http://repo.fusesource.com/nexus/content/groups/public/org/fusesource/esb/fuse-esb/7.0.0.fuse-061/) or later

    Start the server
    bin/fuseesb or bin/fuseesb.bat

And run the following command in the console

    shell:source mvn:org.fusesource.fabric.fabric-examples.fabric-camel-cluster/features/7.0.0.fuse-061/karaf/installer

    If you want to modify the script before sourcing it, you can find it in the Fabric examples source at ${FABRIC_HOME}/fabric-examples/etc/install-fon.karaf
    So you can source it directly using the following command

        shell:source file:///${FABRIC_HOME}/etc/install-fabric-camel-on-fuse-esb-7.karaf


    2) Then you will see the following messages on the console

    Install Fabric-camel example on Fuse ESB 7
    Create Fabric ensemble - Zookeeper registry
    Create fabric-camel-cluster profile
    add repositories, features, ... to the fabric-camel-cluster profile
    Create camel-cluster-port-9090 profile extending fabric-camel-cluster to specify the property of the port number used by the server
    Create camel-cluster-port-9191 profile extending fabric-camel-cluster
    Create fabric container camel-9090
    The following containers have been created successfully:
	    camel-9090
    Create fabric container camel-9191
    The following containers have been created successfully:
	    camel-9191
    Create fabric-camel-consumer profile
    Create fabric container camel-client
    The following containers have been created successfully:
	camel-client

# OUTPUT

To verify that the camel client consuming the services receive well responses randomly, connect
to the Fabric container camel-client and check the log

    2012-06-28 13:51:14,977 | INFO  | #1 - timer://foo | fabric-client                    | 92 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Fabric Container : camel-9090
    2012-06-28 13:51:16,979 | INFO  | #1 - timer://foo | fabric-client                    | 92 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Fabric Container : camel-9191
    2012-06-28 13:51:20,980 | INFO  | #1 - timer://foo | fabric-client                    | 92 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Fabric Container : camel-9191

# VIDEO
=======
    In the root container you should receive messages from the 2 instances deployed
    in camel-9090 and camel-9191 and loadbalanced

    2012-06-20 14:15:39,217 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9090
    2012-06-20 14:15:49,219 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9191
    2012-06-20 14:15:59,215 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9090
    2012-06-20 14:16:09,213 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9191

Enjoy Cloud Integration with Camel !
=======
