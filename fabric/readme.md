# Fabric

## Description

Fuse Fabric is a distributed configuration, management and provisioning system for using
[Apache Karaf](http://karaf.apache.org/), [Apache ServiceMix](http://servicemix.apache.org/)
and [Fuse](http://fusesource.com/) in a public or private cloud.

## Synopsis

Apache Karaf provides an OSGi runtime, but it lacks some clustering support.
Fuse Fabric aims to provide the needed infrastructure to manage the configuration
and provisioning of multiple Karaf nodes.

## Architecture

Fuse Fabric uses on [Apache ZooKeeper](http://zookeeper.apache.org/), which is highly reliable distributed coordination service,
to store the cluster configuration and node registration.

Fabric defines a notion of profile that can be applied to Karaf nodes.  A profile consist
of a list of configurations that will be provided to ConfigAdmin.  Multiple profiles can
be associated to a given node, allowing a given node to serve multiple purposes.
Profiles can also have inheritance so that parts of configuration can be shared across multiple
profiles.  The overall list of configurations is computed using an overlay mechanism which allow
a profile to override values from its parents, which provides power and flexibility.
Those profiles are stored in ZooKeeper, hence automatically and immediately propagated to all
nodes which can refresh the configurations as needed.

Fabric defines a provisioning agent relying on Karaf features through ConfigurationAdmin.
The list of features to be installed on a given node is retrieved from a known configuration
by the agent and the features are installed / uninstalled as needed.

[Apache ZooKeeper]: http://zookeeper.apache.org/

## Getting started

Go to the [website](http://fabric.fusesource.org/documentation/getting-started.html) for a quick start guide.

## Project Links

* [Project Home](http://fabric.fusesource.org/)
* [Documentation](http://fabric.fusesource.org/documentation/)
* [Downloads](http://fabric.fusesource.org/download.html)
* [GitHub](http://github.com/fusesource/fabric/tree/master)
* [Support](http://fabric.fusesource.org/support.html)
* [Community](http://fabric.fusesource.org/community.html)

## Maven modules description

* etc : shell scripts files used by demo/examples to provision/deploy fabric on Apache Karaf

* fab : parent project containing fab modules to deploy Fabric Archive Bundles on Karaf/Service (http://fabric.fusesource.org/documentation/bundle/)

* fabric-activemq : fabric container connector for ActiveMQ

* fabric-apollo : fabric container connector for Apollo (Next generation of ActiveMQ Middelware). Facilitate the discovery of AMQ in a fabric

* fabric-assemblies : Fabric assembly modules

    * fabric8-karaf : Generates a Karaf distribution which has Fabric preinstalled.

* fabric-camel : fabric container connector for camel. Allow to loadbalance requests between camel endpoints deployed in different fabric machines

* fabric-camel-c24io : C24 Camel Transformer component

* fabric-camel-c24io-distro : Distro for c24 component

* fabric-camel-dslio : Camel DSL IO Api - Goal ????

* fabric-command : Karaf fabric commands used to create a zookeeper registry, manage containers or profile, admin agent

* fabric-configadmin : Bridge between osgi configadmin and zookeeper

* fabric-core : Core API of Fabric, container and monitoring stuffs (JMX)

* fabric-core-agent-jclouds : An container provider for creating containers in the cloud via jclouds.

* fabric-core-agent-ssh : Extension of the Fabric core project to support ssh deployment of container

* fabric-cxf fabric-agent-connector for CXF

* fabric-dosgi : Implementation of the specification Distributed OSGI for Fabrix. Uses

* fabric-examples : Examples / demo about Fabric Camel, DOSGI, ActiveMQ, ...

* fabric-groups : Fabric API used to create Zookeeper cluster

* fabric-itests : Integration tests

* fabric-linkedin-zookeeper : only contains a pom.xml ????

* fabric-maven-proxy : Maven proxy which can be used by a remote fabric container to get the artifacts to be deployed

* fabric-monitor : Monitoring service which will allow using JMX to send information about fabric container to the FON console ????

* fabric-scala : Scala maven settings for doing ??

* fabric-security : Security extensions containing

    * fabric-security-sso-client : A simple utility that uses the OpenAM REST API to provide authentication/authorization
    * fabric-security-sso-activemq : A single sign-on JAAS module and plugin for ActiveMQ 5.x that delegates authentication and authorization to an OpenAM Server.

* fabric-util : Only contain a target directory ????

* fabric-website : Content used to generate the web site documentation about Fabric on fabric.fusesource.org

* fabric-zookeeper : Implementation of Zookeeper for Karaf/OSGI world

* fabric-zookeeper-commands : Karaf fabric commands used to manage (get/list/delete, ...) zookeeper entries in the registry

* fabric-zookeeper-spring : Zookeeper Spring integration
