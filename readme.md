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

Checkout Fabric:
    git clone git://github.com/fusesource/fabric.git
    cd fabric

Build Fabric:
    mvn install

To create a ZooKeeper server, run the following commands in Karaf (2.2.x) console:
    features:addurl mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features
    features:install fabric-commands
    fabric:zk-cluster root

You can now use the ZooKeeper commands and see that the karaf instance has been automatically
registered as an agent:
    karaf@root> fabric:list-agents
    root: alive=true
    karaf@root>

Let's create a new child agent:
    karaf@root> fabric:create-agent test root
    ...
Check that the agent has been created (it can take a few seconds):
    karaf@root> fabric:list-agents
    test: alive=true, parent=root
    root: alive=true

Now, let's create a profile webserver, inheriting the default profile:
    karaf@root> zk:create /fabric/configs/versions/base/profiles/webserver default
And associate the karaf war feature to it:
    karaf@root> zk:create -r /fabric/configs/versions/base/profiles/webserver/org.fusesource.fabric.agent/repository.karaf mvn:org.apache.karaf.assemblies.features/standard/2.2.1-SNAPSHOT/xml/features
    karaf@root> zk:create -r /fabric/configs/versions/base/profiles/webserver/org.fusesource.fabric.agent/feature.war war

Last, let's associate the test agent we've created with the webserver profile:
    karaf@root> zk:set /fabric/configs/versions/base/agents/test webserver

Check that the feature has been deployed:
    karaf@root> fabric:connect test
    ...
    karaf@test> osgi:list
    ...

## Project Links

* [Project Home](http://fabric.fusesource.org/)
* [Documentation](http://fabric.fusesource.org/documentation/)
* [Downloads](http://fabric.fusesource.org/download.html)
* [GitHub](http://github.com/fusesource/fabric/tree/master)
* [Support](http://fabric.fusesource.org/support.html)
* [Community](http://fabric.fusesource.org/community.html)
