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
a cluster environment.  We will deploy a fabric camel route on Fuse ESB 7 / JBoss Fuse instance

    <route id="fabric-slave">
      <from uri="timer://foo?fixedRate=true&amp;period=10000"/>
      <setBody>
          <simple>Hello from Fabric Client to group "Cluster"</simple>
      </setBody>
      <to uri="fabric-camel:cluster"/>
      <log message=">>> ${body} : ${header.karaf.name}"/>
    </route>

and send messages to a fabric group called "cluster". Two camel routes deployed on fabric
containers and exposing HTTP Server as fabric camel endpoint will be able to answer to the client.

     <route id="fabric-server">
        <from uri="fabric-camel:cluster:jetty:http://0.0.0.0:[[portNumber]]/fabric"/>
        <log message="Request received : ${body}"/>
        <setHeader headerName="karaf.name">
            <simple>${sys.karaf.name}</simple>
        </setHeader>
        <transform>
            <simple>Response from Zookeeper agent</simple>
        </transform>
     </route>

The fabric endpoint allows to associate the jetty component to a logical group name. In our example, this group is called "cluster".
When the Fabric agent will discover that a fabric endpoint must be published, then it will call the zookeeper registry and add an entry containing
the location of the endpoint. The result hereafter shows you what Zookeeper has finally registered for 2 different endpoints under "cluster".

    registry/camel/endpoints/cluster/00000000000 = jetty:http://0.0.0.0:9191/fabric
    registry/camel/endpoints/cluster/00000000001 = jetty:http://0.0.0.0:9090/fabric


The camel producer (= client), instead of calling directly the camel endpoint as usal, will call a Fabric endpoint using as key
the group name "cluster". Then a lookup occurs in the Zookeeper registry to question it and find which endpoints have been registered.
The Camel component will get a list of endpoints that it will use randomly to publish every 2s a message
"Hello from Fabric Client to group cluster"

Moreover, the component will load balance messages to the list of endpoints published

![fabric-camel.png](https://github.com/fusesource/fuse/raw/master/fabric/fabric-examples/fabric-camel-cluster-loadbalancing/fabric-camel.png)

# COMPILING

    cd fabric-examples/fabric-camel-cluster-loadbalancing
    mvn clean install

# RUNNING ON FUSE ESB 7 / JBoss Fuse

The project will deploy fabric camel routes on Fuse ESB 7 / JBoss Fuse instance but you can also install a Fabric distro

1) Before you run Fuse ESB 7 you might like to set these environment variables...

    export JAVA_PERM_MEM=64m
    export JAVA_MAX_PERM_MEM=512m

In an install of Fuse ESB 7 (http://repo.fusesource.com/nexus/content/groups/public/org/fusesource/esb/fuse-esb/7.1.0.fuse-047/) or JBoss Fuse (https://access.redhat.com/downloads/)

    Start the server
    bin/fuseesb or bin/fuseesb.bat

And run the following commands in the console

    shell:source mvn:org.fusesource.examples.fabric-camel-cluster/features/99-master-SNAPSHOT/fabric/installer
    shell:source mvn:org.fusesource.examples.fabric-camel-cluster/features/99-master-SNAPSHOT/karaf/installer

    If you want to modify the script before sourcing it, you can find it in the Fabric examples source at ${FABRIC_HOME}/fabric-examples/etc
    So you can source it directly using the following command

        shell:source file:///${FABRIC_HOME}/etc/install-fabric-camel-loadbalancer-example.karaf


    2) Then you will see the following messages on the console

    JBossFuse:karaf@root> shell:source mvn:org.fusesource.examples.fabric-camel-cluster/features/99-master-SNAPSHOT/fabric/installer
    Create Fabric server (Zookeeper registry)
    Using specified zookeeper password:admin

    JBossFuse:karaf@root> shell:source mvn:org.fusesource.examples.fabric-camel-cluster/features/99-master-SNAPSHOT/karaf/installer
    Install Fabric Camel Loadbalancer example on JBoss Fuse
    Create profile that we use for cluster of camel endpoints (Jetty)
    Add repositories and features to the camel-cluster profile
    Create port-9090 profile extending camel-cluster to specify the port number used by the server
    Create port-9191 profile extending camel-cluster
    Create a first fabric container (camel-9090) for the cluster
    The following containers have been created successfully:
        Container: camel-9090.
    Create a second fabric container (camel-9191) for the cluster
    The following containers have been created successfully:
        Container: camel-9191.
    Create Fabric Camel Consumer profile to deploy the client calling the cluster
    Create fabric container camel-client
    The following containers have been created successfully:
        Container: camel-client.
    Install zookeeper commands to display data registered

# OUTPUT

To verify that the camel client consuming the services receive well responses randomly, connect
to the Fabric container camel-client and check the log

    2013-03-15 18:05:08,022 | INFO  | #0 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 96 - org.apache.camel.camel-core - 2.10.0.redhat-60015 | >>> Response from Fabric Container : camel-9191
    2013-03-15 18:05:10,018 | INFO  | #0 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 96 - org.apache.camel.camel-core - 2.10.0.redhat-60015 | >>> Response from Fabric Container : camel-9191
    2013-03-15 18:05:12,019 | INFO  | #0 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 96 - org.apache.camel.camel-core - 2.10.0.redhat-60015 | >>> Response from Fabric Container : camel-9090
    2013-03-15 18:05:14,021 | INFO  | #0 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 96 - org.apache.camel.camel-core - 2.10.0.redhat-60015 | >>> Response from Fabric Container : camel-9090


Enjoy Clustering with Fabric Camel !
====================================
