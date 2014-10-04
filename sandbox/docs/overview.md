## Overview

**fabric8** (pronounced _fabricate_) is designed to make it really easy to deploy your Java integration solutions and services on a number of machines, containers, processes, and JVMs. Then once they are provisioned you can easily:

* visualise what is running to understand your platform
* easily configure and monitor whats running
* automatically discover services running in the fabric (through a runtime registry)
* load balance
* provide leader election, master/slave coordination
* scale up or down specific [profiles](profiles.html) based on [RHQ](http://www.jboss.org/rhq)/[JON](http://www.redhat.com/products/jbossenterprisemiddleware/operations-network/) alerts based on key performance metrics (e.g. queue depths, throughput rates, latency etc)
* make configuration, composition or version changes in a big bang approach (all relevant containers updates change immediately on each change) or
* [rolling upgrades](rollingUpgrade.html) so you can stage when things update and which containers you wish to update when.

We've designed **fabric8**  to be very lightweight (just run one or a few JVMs, no database required) and cloud friendly. So fabric8 works great whether you:

* run Java processes directly on your own hardware without any kind of cloud or virtualisation technology
* use a PaaS (Platform as a Service) such as <a href="https://www.openshift.com/products/online">OpenShift Online</a> for the public cloud or <a href="https://www.openshift.com/products/enterprise">OpenShift Enterprise</a> for the private on premise cloud
* you are using OpenStack as an IaaS (Infrastructure as a Service) to create compute nodes and manage storage and networks
* use Amazon Web Services, Rackspace or some other IaaS directly to run your services
* use [docker](http://docker.io/) containers to abstract how you run services (as a virtualisation alternative)
* use an open hybrid cloud of all the above.

### Motivation

Increasingly systems are getting bigger, more complex, dynamic and cloud-ready. Namely we're booting up systems quicker and we want them to be more automated and elastic without the need for manual intervention. The old days of having 2 boxes with a few XML config files on each box managed by hand are fast becoming the death to business relying heavily on IT (all businesses).

When things are more elastic, cloud-ready, and dynamic, host names and IP addresses tend to be generated on the fly; so your system needs to use more _discovery_ to find things.

e.g. if you're using some messaging you should discover dynamically where your message brokers are at runtime rather than hand-coding some host names / IP addresses in config files. This makes it easier to provision and configure in a more agile way; plus you can dynamically add more message brokers as you need them (so you become more elastic).

In addition, if each machine has a separate set of config files edited by hand, as soon as the number of folks in your team and number of machines increases, things become unmanageable. You want all configuration centrally managed and audited with version control and the ability to see who changed what, when, see a diff of exactly what changed, and to revert bad changes, do [rolling upgrades](rollingUpgrade.html) and so forth.

The Ideal workflow is to perform _continuous deployment_ of changes to configuration and software versions; when those changes have passed the _continuous integration_ tests (maybe with people voting too along with Jenkins) to auto-merge changes from the edit repo into the production repo and have the fabric update itself dynamically.

### History
ServiceMix is also an open-source ESB based on Apache Camel and ActiveMQ. So how does this relate to Fabric8?

ServiceMix is the genesis of the current JBoss Fuse/Fabric8. It started off 9 or so years ago as an implementation of an EnterpriseServiceBus (ESB) based on the Java Business Integration spec. It’s goal was to provide a pluggable component architecture with a normalized messaging backbone that would adhere to standard interfaces and canonical XML data formats. ServiceMix gained a lot of popularity, despite JBI being a overly ceremonious spec (lots and lots of XML descriptors, packaging demands, etc). But, despite most products/projects offering integration services as a large, complex container, the need for routing, transformation, integrating with external systems, etc. shows up outside of that complex “ESB” environment as well :)

Around the SMX 3.x and 4.x timeframe, the project underwent some major refactoring. The JBI implementation was ripped out and simplified with routing/mediation DSL that would later become Apache Camel. This way the “heart” of the “ESB” could be used in other projects (ActiveMQ, stand alone, etc). Additionally, the core container also moved away from JBI and toward OSGi. Still later, the actual OSGi container was refactored out into its own project, now known as Karaf. So ServiceMix became less its own project and really a packaging of other projects like ActiveMQ, Karaf (which used to be core SMX) and Camel (which used to be core SMX). The older versions of JBoss Fuse (Fuse ESB/Fuse Enterprise) where basically a hardening of SMX which was already a repackaging of some Apache projects. Additionally a lot of the core developers working on SMX also moved toward contributing to the constituent pieces and not necessarily the core SMX.

Fabric8 takes the “ESB” or “integration” spirit of ServiceMix and adds a nice management UI (HawtIO), and all of the DevOpsy, and paints a clear path toward large-scale deployments and _continuous delivery_

### Concepts

The concepts behind **fabric8** are pretty simple, they are:


#### Git for configuration

Fabric8 uses [git](http://git-scm.com/) as the _distributed version control_ mechanism [for all configuration](git.html). This means that all changes are versioned and replicated onto each machine with a full audit history of who changed what and when.

In addition its easy to reuse any of the existing git tooling to perform diffs, merges and continuous integration. For more detail see [how to use git and fabric8](git.html)

Fabric8 actually implements a [distributed git fabric](git.html) with no single point of failure or configuration change loss. A master node is elected which becomes the remote git repository; all configuration changes are pushed to the master and pulled from it so each node stays in sync. If the master node dies, the fabric fails over to another node. So there is no single point of failure, central server or infrastructure required (just a couple of JVMs is all you need for fabric8).

We make use of git branches to implement [rolling upgrades](rollingUpgrade.html); each version maps to a branch in git. So we can individually move containers from version to version (or branch to branch) to implement rolling upgrades.

#### Use Profiles for DRY configuration

Rather than configuring each [container](agent.html) (i.e. JVM or process) individually, we use [profiles](profiles.html) to represent a collection of containers; so that you can configure a group of containers in a nice DRY way.

You can combine [profiles](profiles.html) into a container so you can keep your configuration DRY. For example you can decide to colocate services together (putting multiple [profiles](profiles.html) into a container) when they make sense; or separate them into different containers.

Or you can use inheritance; so you can configure, say, the ActiveMQ version to use globally; then have a profile which overrides the global configuration of ActiveMQ, for use on a big linux box rather than a small windows box; override the threading or memory usage configuration; while reusing other parts of the configuration.

#### ZooKeeper for the runtime registry

Fabric8 uses [Apache ZooKeeper](http://zookeeper.apache.org/) (from the [Hadoop](http://hadoop.apache.org/) ecosystem) as a way to perform _runtime discovery_ of containers (machines, processes, JVMs) and for coordination (electing leaders, implementing master/slave, sharding or federation of services).
