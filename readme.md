# ![Fabric]

## Description

[Fabric][] is a set of components for providing a clustering solution for [Apache Karaf].

[Apache Karaf]: http://karaf.apache.org/

## Synopsis

Apache Karaf provides an OSGi runtime, but it lacks some clustering support.
[Fabric] aims to provide the needed infrastructure to manage the configuration
and provisioning of multiple Karaf nodes.

## Architecture

[Fabric] rely on [Apache ZooKeeper], which is highly reliable distributed coordination service,
to store the cluster configuration and node registration.

[Fabric] defines a notion of profile that can be applied to Karaf nodes.  A profile consist
of a list of configurations that will be provided to ConfigAdmin.  A single can be associated
to multiple profiles, allowing a given node to serve multiple purposes.   Profiles can also
have inheritence so that parts of configuration can be shared across multiple profiles.

[Fabric] defines a provisioning agent relying on Karaf features through ConfigurationAdmin.
The list of features to be installed on a given node is retrieved from a known configuration
by the agent and the features are installed / uninstalled as needed.



[Apache ZooKeeper]: http://zookeeper.apache.org/

## Project Links

* [Project Home](http://fabric.fusesource.org/)
* [Release Downloads](http://fabric.fusesource.org/downloads/index.html)
* [GitHub](http://github.com/fusesource/fabric/tree/master)
* [Issue Tracker](http://fusesource.com/issues/browse/FABRIC)
* [Mailing Lists](http://fusesource.com/forge/projects/FABRIC/mailing-lists)