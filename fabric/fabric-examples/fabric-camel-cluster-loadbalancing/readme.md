﻿INTRODUCTION
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

Additionally, the loadbalancing mechanism of fabric camel will be displayed as the response will come randomly
from one of the instance configured

![fabric-camel.png](https://github.com/fusesource/fuse/raw/master/fabric/fabric-examples/fabric-camel-cluster-loadbalancing/fabric-camel.png)

COMPILING
=========
    cd fabric-examples/fabric-camel-cluster-loadbalancing
    mvn clean install

RUNNING ON FUSE ESB 7
=====================
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
    Create fabric-camel-cluster profile in zookeeper
    add repositories, features, ... to the fabric-camel-cluster profile
    Create camel-cluster-port-9090 profile extending fabric-camel-cluster to specify the property of the port number used by the server
    Create camel-cluster-port-9191 profile extending fabric-camel-cluster
    Create fabric container camel-9090 and wait 10s that it is created and configured
    The following containers have been created successfully:
    	camel-9090
    Create fabric container camel-9191
    The following containers have been created successfully:
    	camel-9191
    Installing camel features repository and deploy camel on the root instance
    Installing camel fabric demo features repository and deploy camel-client
    install camel-client which will connect to the remote instances

OUTPUT
======

    In the root container you should receive messages from the 2 instances deployed
    in camel-9090 and camel-9191 and loadbalanced

    2012-06-20 14:15:39,217 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9090
    2012-06-20 14:15:49,219 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9191
    2012-06-20 14:15:59,215 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9090
    2012-06-20 14:16:09,213 | INFO  | #1 - timer://foo | fabric-client                    | 138 - org.apache.camel.camel-core - 2.9.0.fuse-7-061 | >>> Response from Zookeeper agent : camel-9191



