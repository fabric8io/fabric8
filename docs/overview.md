## Overview

**fabric8** is designed to make it really easy to deploy your java integration solutions and services on a number of machines, processes and JVMs. Then once they are provisioned you can easily:

* visualise what is running to understand your platform
* easily manage and monitor whats running and easily scaling up or down specific [profiles](#/site/book/doc/index.md?chapter=profiles_md) based on [RHQ](http://www.jboss.org/rhq)/[JON](http://www.redhat.com/products/jbossenterprisemiddleware/operations-network/) alerts based on key performance metrics (e.g. queue depths, throughput rates, latency etc)
* make configuration, composition or version changes in a big bang approach (all relevant containers updates change immediately on each change) or via [rolling upgrades](#/site/book/doc/index.md?chapter=rollingUpgrade_md) so you can stage when things update and which containers you wish to update when.

We've designed **fabric8** docker to be very lightweight (just run one or a few JVMs, no database required) and cloud friendly. So fabric8 works great whether you:

* run Java processes directly on your own hardware without any kind of cloud or virtualisation technology
* use a PaaS (Platform as a Service) such as <a href="https://www.openshift.com/products/online">OpenShift Online</a> for the public cloud or <a href="https://www.openshift.com/products/enterprise">OpenShift Enterprise</a> for the private on premise cloud
* you are using OpenStack as an IaaS (Infrastructure as a Service) to create compute nodes and manage storage and networks
* use Amazon Web Services, Rackspace or some other IaaS directly to run your services
* use [docker](http://docker.io/) containers to abstract how you run services (as a virtualisation alternative)
* use an open hybrid cloud of all the above.

### Motivation

Increasingly systems are getting bigger, more complex, dynamic and cloud-ready. Namely we're booting up systems quicker and we want them to be more agile and elastic. The old days of having 2 boxes with a few XML config files on each box managed by hand are fast becoming a code smell.

When things are more elastic, cloud-ready and dynamic; host names and IP addresses tend to be generated on the fly; so your system needs to use more _discovery_ to find things.

e.g. if you're using some messaging you should discover dynamically where your message brokers are at runtime rather than hand-coding some host names / IP addresses in config files. This makes it easier to provision, in a more agile way; plus you can dynamically add more message brokers as you need them (so you become more elastic).

In addition; if each machine has a separate set of config files editted by hand; as soon as the number of folks in your team and number of machines increase; things become unmanageable. You want all configuration centrally managed and audited; with version control and the ability to see who changed what when, see a diff of exactly what changed and to revert bad changes, do [rolling upgrades](#/site/book/doc/index.md?chapter=rollingUpgrade_md) and so forth.

Ideally perform _continuous deployment_ of changes to configuration and software versions; when those changes have passed the _continuous integration_ tests (maybe with people voting too along with Jenkins) to auto-merge changes from the edit repo into the production repo and have the fabric update itself dynamically.

### Concepts

The concepts behind **fabric8** are pretty simple, they are:

#### ZooKeeper for the runtime registry

Fabric8 uses [Apache ZooKeeper](http://zookeeeper.apache.org/) (from the [Hadoop](http://hadoop.apache.org/) ecosystem) as a way to perform _runtime discovery_ of containers (machines, processes, JVMs) and for coordination (electing leaders, implementing master/slave, sharding or federation of services).

#### Git for configuration

Fabric8 uses [git](http://git-scm.com/) as the _distributed version control_ mechanism for all configuration.  This means that all changes are versioned and replicated onto each machine with a full audit history of who changed what and when.

In addition its easy to reuse any of the existing git tooling to perform diffs, merges and continuous integration.

Fabric8 actually implements a distributed git fabric with no single point of failure or configuration change loss. A master node is elected which becomes the remote git repository; all configuration changes are pushed to the master and pulled from it so each node stays in sync. If the master node dies, the fabric fails over to another node. So there is no single point of failure, central server or infrastructure required (just a couple of JVMs is all you need for fabric8).

We make use of git branches to implement [rolling upgrades](#/site/book/doc/index.md?chapter=rollingUpgrade_md); each version maps to a branch in git. So we can individually move containers from version to version (or branch to branch) to implement rolling upgrades.

#### Use Profiles for DRY configuration

Rather than configuring each [container](#/site/book/doc/index.md?chapter=agent_md) (i.e. JVM or process) individually, we use [profiles](#/site/book/doc/index.md?chapter=profiles_md) to represent a collection of containers; so that you can configure a group of containers in a nice DRY way.

You can combine profiles into a container so you can keep your configuration DRY. For example you can decide to colocate services together (putting multiple profiles into a container) when they make sense; or separate them into different containers.

Or you can use inheritance; so you can configure, say, the ActiveMQ version to use globally; then have a profile which overrides the global configuration of ActiveMQ, for use on a big linux box rather than a small windows box; override the threading or memory usage configuration; while reusing other parts of the configuration.

