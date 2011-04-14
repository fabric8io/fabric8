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
