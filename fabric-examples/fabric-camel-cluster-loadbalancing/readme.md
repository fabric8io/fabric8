INTRODUCTION
============

This example demonstrates the provisioning feature of fuseSource Fabric based on profiles
defined in zookeeper configuration registry and how we can use the fabric-camel component in
a cluster environment.  We will deploy a fabric camel route on one karaf instance

    <route id="fabric-slave">
      <from uri="timer://foo?fixedRate=true&amp;period=10000"/>
      <setBody>
          <simple>Hello from Zookeeper server</simple>
      </setBody>
      <to uri="fabric:cheese"/>
      <log message=">>> ${body} : ${header.karaf.name}"/>
    </route>

and sends messages to a fabric called cheese while two other fabric camel routes deployed on zookeeper
agents and exposing HTTP Servers will be able to answer to the client.

     <route id="fabric-server">
        <from uri="fabric:cheese:jetty:http://0.0.0.0:{{portNumber}}/fabric"/>
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

COMPILING
=========
cd fabric-examples/fabric-camel-cluster-loadbalancing
mvn clean install

RUNNING
=======
1) Before you run Karaf you might like to set these environment variables...

    export JAVA_PERM_MEM=64m
    export JAVA_MAX_PERM_MEM=512m

In an install of Karaf 2.2.1 or later such as from https://repository.apache.org/content/groups/snapshots-group/org/apache/karaf/apache-karaf/2.2.1-SNAPSHOT/
run the following command...

    shell:source mvn:org.fusesource.fabric.fabric-examples.fabric-camel-cluster/features/1.0-SNAPSHOT/karaf/installer

If you want to modify the script before sourcing it, you can find it in the Fabric examples source at ${FABRIC_HOME}/fabric-examples/etc/install-fon.karaf
So you can source it directly using the following command

    shell:source file:///${FABRIC_HOME}/etc/install-fabric-camel.karaf


2) Then you will see the following messages on the console

Waiting a couple of seconds for the fabric-commands to start
Waiting a couple of seconds for the zookeeper server to start
Installing camel features repository and deploy camel
Installing camel fabric demo features repository and deploy camel-master
Create fabric profile
add repositories, features to be deployed in the store
Create fabric agent


OUTPUT
======

- Connect to the agent
fabric:connect zk-9090 or fabric:connect zk-9191

- Check that the camel fabric route can consume messages from camel master !


and that master receive message from slave


Enjoy the first fabric-camel demo (Created the 06th May 2011) !
