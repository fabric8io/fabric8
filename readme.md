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
of a list of configurations that will be provided to ConfigAdmin.  A single can be associated
to multiple profiles, allowing a given node to serve multiple purposes.   Profiles can also
have inheritance so that parts of configuration can be shared across multiple profiles.

Fabric defines a provisioning agent relying on Karaf features through ConfigurationAdmin.
The list of features to be installed on a given node is retrieved from a known configuration
by the agent and the features are installed / uninstalled as needed.

## Project Links

* [Project Home](http://fabric.fusesource.org/)
* [Documentation](http://fabric.fusesource.org/documentation/)
* [Downloads](http://fabric.fusesource.org/download.html)
* [GitHub](http://github.com/fusesource/fabric/tree/master)
* [Support](http://fabric.fusesource.org/support.html)
* [Community](http://fabric.fusesource.org/community.html)