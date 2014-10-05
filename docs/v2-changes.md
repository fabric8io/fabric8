## Differences between 1.x and 2.x

In fabric8 1.x we assumed only a platform with a JVM and built lots of abstractions that were very similar to [Kubernetes](http://kubernetes.io). In fabric8 2.x we have decided to assume a base of [Docker](http://docker.io/), [Kubernetes](http://kubernetes.io) and the [Kubernetes extensions defined in OpenShift Origin](). For non-linux platforms which don't support docker we have a [Docker Emulation Layer](emulation.html).

Because of this approach we do quite a few things a little differently at the implementation layer. In fabric8 1.x we had to do a lot of work to replicate the kinds of things Kubernetes gives us; things like discovery of services, wiring, auto   scaling etc.

However as an end user writing, say, Java applications or using technologies like ActiveMQ or Camel things should seem pretty similar; though in many ways things get much simpler now (as discovery, wiring and packaging is now simpler and more standardised).

Here are the main differences:

### Service Discovery

Instead of using a ZooKeeper registry in 1.x to store all service endpoints so that we could inject them into other services using property resolvers; we instead use the [Services](services.html) abstraction in kubernetes. This means clients of a network service just bind to a fixed port on the local network; there's no need to have any complex wiring inside the application.

This radically simplifies applications. For example any application using ActiveMQ just needs to bind to the localhost and port of the region of the ActiveMQ cluster. For many Camel endpoints, the endpoint just needs to choose which port number maps to the service it needs to bind to for things like databases.


### Profiles versus Application Templates (Apps)

In fabric8 1.x we focused on profiles in a wiki which are used to represent configuration and metadata which provides a mechanism to build profiles into a container.

In fabric8 2.x we default to using [Kubernetes Application Templates](apps.html) to define the kubernetes JSON metadata ([Pods](pods.html), [Replication Controllers](replicationControllers.html), [Services](services.html)).

In addition to support the ability to modify source or metadata inside a wiki / git repository (e.g. camel routes or Java code) we default to using the [Kubernetes source to image extension](builds.html). i.e. Profiles map to a build which has a git repository; this can be viewed/edited via the wiki, a web based IDE or traditional IDE or editor.

So source projects are editted; they result in builds which then generate container images which are then used by deployments.


### Auto Scaling

In fabric8 1.x we used requirements we specified on a Profile basis. In 2.x we use [Replication Controller definitions in JSON](replicationControllers.html) to specify how many instances of a certain [pod](pods.html) (container) we need to run.


### Ports

In [Kubernetes](http://kubernetes.io) each pod (or container) gets its own IP address; and the internal container ports and external host ports are then constant; making things much easier to use and wire together.

This is quite different to using fabric8 1.x where depending on the underlying container provider port allocations were different. e.g. by default host and container ports were dynamically generated; for docker internal container ports were constant but external host ports were dynamic.