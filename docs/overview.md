## Overview

**fabric8** is designed to make it really easy to deploy your java integration solutions and services on a number of machines, processes and JVMs. Then once they are provisioned you can easily:

* visualise what is running to understand your platform
* easily manage and monitor whats running and easily scaling up or down specific [profiles](#/site/book/doc/index.md?chapter=profiles_md) maybe based on JON alerts based on key performance metrics (e.g. queue depths, throughput rates, latency etc)
* make configuration, composition or version changes in a big bang approach (all relevant containers updates change immediately on each change) or via [rolling upgrades](#/site/book/doc/index.md?chapter=rollingUpgrade_md) so you can stage when things update and which containers you wish to update when.

We've designed **fabric8** docker to be very lightweight (just run one or a few JVMs) and cloud friendly. So fabric8 works great whether you:

* run Java processes directly on your own hardware without any kind of cloud or virtualisation technology
* you are using OpenStack as an IaaS (Infrastructure as a Service) to create compute nodes and manage storage and networks
* use Amazon Web Services, Rackspace or some other IaaS directly to run your services
* use a PaaS (Platform as a Service) such as <a href="https://www.openshift.com/products/online">OpenShift Online</a> for the public cloud or <a href="https://www.openshift.com/products/enterprise">OpenShift Enterprise</a> for the private on premise cloud
* use [docker](http://docker.io/) containers to abstract how you run services (as a virtualisation alternative)
* use an open hybrid cloud of all the above.

### Motivation

Increasingly systems are getting bigger, more complex, dynamic and cloud-ready. Namely we're booting up systems quicker and we want them to be more agile and elastic. The old days of having 2 boxes with a few XML config files on each box managed by hand are fast becoming a code smell.

When things are more elastic, cloud-ready and dynamic; host names and IP addresses tend to be generated on the fly; so your system needs to use more _discovery_ to find things.

e.g. if you're using some messaging you should discover dynamically where your message brokers are at runtime rather than hand-coding some host names / IP addresses in config files. This makes it easier to provision, in a more agile way; plus you can dynamically add more message brokers as you need them (so you become more elastic).

In addition; if each machine has a separate set of config files editted by hand; as soon as the number of folks in your team and number of machines increase; things become unmanageable. You want all configuration centrally managed and audited; with version control and the ability to see who changed what when, see a diff of exactly what changed and to revert bad changes, do [rolling upgrades](#/site/book/doc/index.md?chapter=rollingUpgrade_md) and so forth.

Ideally perform _continuous deployment_ of changes to configuration and software versions; when those changes have passed the _continuous integration_ tests (maybe with people voting too along with Jenkins) to auto-merge changes from the edit repo into the production repo and have the fabric update itself dynamically.

### Concepts

The concepts behind **fabric8** are pretty simple. Its to:

* use [Apache ZooKeeper](http://zookeeeper.apache.org/) (from the Hadoop ecosystem) as a way to perform _runtime discovery_ of containers (machines, processes, JVMs) and for coordination (electing leaders, implementing master/slave, sharding or federation of services).
* use git as a distributed version control mechanism so all configurations and changes are versioned; have full audit history (and tooling to perform diffs, merges and continuous integration) while also using git's distributed nature to implement a distributed git cloud; i.e. no single point of failure, no configuration change is ever lost; if the master git repo dies, it fails over to another node; each node pushes any local changes and pulls any remote changes; so you have a highly available history of all changes by everyone in every branch on all machines; with no single point of failure or central server or infrastructure required (just a couple of JVMs is all you need for fabric8).

Rather than configurating each [container](#/site/book/doc/index.md?chapter=agent_md) (i.e. JVM or process) individually, we use [profiles](#/site/book/doc/index.md?chapter=profiles_md) to represent a collection of containers; so that you can configure a group of containers in one go.

Via inheritance and composition you can combine profiles into containers so you can keep your configuration DRY. For example you can decide to colocate services together (putting multiple profiles into a container) when they make sense; or separate them into differnet containers.

Or you can use inheritence; so you can configure, say, the ActiveMQ version to use globally; then have a profile which overrides the global configuration of ActiveMQ, for use on a big linux box rather than a small windows box; override the threading or memory usage configuration.

