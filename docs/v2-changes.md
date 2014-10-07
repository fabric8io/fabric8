## Changes since Version 1.x

In fabric8 1.x we assumed only a JVM was available and then we build lots of abstractions that are very similar to [Kubernetes](http://kubernetes.io).

In fabric8 2.x we have decided to assume a base of [Docker](http://docker.io/), [Kubernetes](http://kubernetes.io) and the [Kubernetes extensions defined in OpenShift Origin]() and to use those abstractions to create containers and wire them together. For non-linux platforms which don't support docker we have a [Docker Emulation Layer](emulation.html).

Because of this approach we do quite a few things a little differently under the covers. In fabric8 1.x we had to do a lot of work to replicate the kinds of things Kubernetes gives us; things like discovery of services, wiring, auto   scaling etc.

However as an end user writing, say, Java applications or using technologies like ActiveMQ, Camel or containers like Karaf, Tomcat, WildFly or Spring Boot things should seem pretty similar; though in many ways things get much simpler now (as discovery, wiring and packaging is now simpler and more standardised).

The aim of fabric8 on kubernetes is still to support any runtime application server or software distribution at all; though docker containers will be the standard mechanism for packaging and running software. The change from 1.x to 2.x does not mean any application servers are no longer supported; its the reverse - now any docker image is supported ;).

### How does it affect my build?

If you have been using fabric8 1.x up to now and have maven builds making profiles; all that really changes is how the build gets packaged into containers.

i.e. your maven builds which create jars, bundles, wars and so forth are all totally valid and supported.

fabric8 2.x [builds](builds.html) just support a different way to automatically turn your deployment units into docker containers for deployment on fabric8.

In addition the use of [Apps](apps.html) allows you to define replication controllers (to specify how many instances of a container you need) as a single deployment JSON. You can also compose multiple containers together and orchestrate them into a single [App JSON file](apps.html).

Finally thanks to [Services](services.html) the wiring of your components to their services becomes much simpler; usually its just a case of using an environment variable for the service port (with a nice default value).

### Architecture differences

#### Service Discovery

Instead of using a ZooKeeper registry in 1.x to store all service endpoints so that we could inject them into other services using property resolvers; we instead use the [Services](services.html) abstraction in kubernetes. This means clients of a network service just bind to a fixed port on the local network; there's no need to have any complex wiring inside the application.

This radically simplifies applications. For example any application using ActiveMQ just needs to bind to the localhost and port of the region of the ActiveMQ cluster. For many Camel endpoints, the endpoint just needs to choose which port number maps to the service it needs to bind to for things like databases.

#### Profiles versus Apps (Kubernetes Application Templates)

In fabric8 1.x we focused on profiles in a wiki which are used to represent configuration and metadata which provides a mechanism to build profiles into a container.

In fabric8 2.x we default to using [Kubernetes Application Templates](apps.html) to define the kubernetes JSON metadata ([Pods](pods.html), [Replication Controllers](replicationControllers.html), [Services](services.html)).

In addition to support the ability to modify source or metadata inside a wiki / git repository (e.g. camel routes or Java code) we default to using the [Kubernetes source to image extension](builds.html). i.e. Profiles map to a build which has a git repository; this can be viewed/edited via the wiki, a web based IDE or traditional IDE or editor.

So source projects are editted; they result in builds which then generate container images which are then used by deployments.

#### Auto Scaling

In fabric8 1.x we used requirements we specified on a Profile basis. In 2.x we use [Replication Controller definitions in JSON](replicationControllers.html) to specify how many instances of a certain [pod](pods.html) (container) we need to run.

#### Ports

In [Kubernetes](http://kubernetes.io) each pod (or container) gets its own IP address; and the internal container ports and external host ports are then constant; making things much easier to use and wire together.

This is quite different to using fabric8 1.x where depending on the underlying container provider port allocations were different. e.g. by default host and container ports were dynamically generated; for docker internal container ports were constant but external host ports were dynamic.

### FAQ on V2 changes

#### So does that mean V2 is a completely different product?

From the implementation perspective V2 is a big change but we see this as an evolutionary change for users of fabric8 in V2:

 * keeps the same set or runtimes, quickstarts, frameworks, capabilities and console (based on [hawtio](http://hawt.io/))
 * delegates the creation of the docker containers down to the Kubernetes REST API (rather than having lots of different container providers and ways to do it that often caused confusion)
 * switching from the V1 Gateway to using [Kubernetes Services](services.html) via the Kubernetes REST API to provide simpler standard approach to the discovery of services in a more Docker-like way.

In many ways 1.x of fabric8 was a set of quickstarts, runtimes, libraries, a console and a kubernetes-like layer all combined into a single project (with V1 Gateway being very like [Kubernetes Services](services.html). So to create containers we have a REST API and CLI.

So in 2.x of fabric8 we use the exact same quickstarts, runtimes, libraries and console. The web console is still [hawtio](http://hawt.io/); its got all the same tooling for ActiveMQ, Camel, CXF, OSGi; its got a wiki based on a git repository. The difference is it uses the Kubernetes REST API to create containers (rather than the fabric8 1.x one) and uses the kubernetes CLI.

Another side benefit of V2 is that less wiring Java code is required. e.g. any library using ActiveMQ or the camel-activemq component no longer needs to specify a brokerURL or include custom discovery code; it can just set the **AMQ_PORT** environment variable to point to the service port for the broker group (e.g. the regional cluster of ActiveMQ required) so that the code connects to localhost and [Kubernetes Services](services.html) does the rest! Note this also works for all programming languages and clients too.

While the exact details of the [Emulation layer](emulation.html) is to be decided; we could move some of the old code from 1.x for creating containers into the emulator (making a pure Java implementation of kubernetes); or we could just reuse kubernetes code ;).

#### Does that mean there is no need to use etcd / ZK clusters in V2?

Kubernetes/OpenShift ships with etcd today and uses that to manage the minions. So a fabric8 cluster may be able to reuse the underlying etcd to do things like master election, partitioning etc.

Though depending on multi tenant requirements; applications might not be allowed to reuse the same etcd as an Kubernetes/OpenShift  installation. e.g. OpenShift Online with 2 million apps probably isn't gonna let any user application just read and write lots of data to the global etcd ;).

So there will still be a need to provision clusters of etcd or ZooKeeper for use inside an application. Given the use of etcd inside Kubernetes/OpenShift then it might be more typical to create sub-clusters (or have a kinda of partitioned etcd cluster).

Though from the fabric8 project we're aiming to provide an abstraction layer so when master election / partitionining is required in Java code we can use either etcd or ZK clusters; and can - if allowed - reuse the same underlying etcd cluster.

Incidentally the V2 way to provision an ensemble of ZooKeeper or etcd cluster is probably going to be via an [App](apps.html) which makes it easier to provision; then clients can use [Services](services.html) to connect to it. So V2 should make the creation of ensembles easier and more standardised across the hybrid clouds.

#### Is my application server or framework still supported in V2?

Absolutely! :) In many ways the move to V2 makes it much easier to support any language, framework or application server as we can reuse all of the Docker ecosystem of images in V2.

So nothing is going away. V2 just standardises things to make them easier to consume, describe, support and makes things more reusable. (e.g. you can take your docker container image to anywhere that can run docker and take your [App JSON](apps.html) to any Kubernetes environment (like OpenShift Online, OpenShift Enterprise, Google Compute Engine, Azure, VMWare etc). They'd be easy to port to run natively on any place docker runs too really (e.g. EC2).