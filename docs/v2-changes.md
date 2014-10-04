## Differences between 1.x and 2.x

The main difference in fabric8 2.x is that we assume the runtime platform is based on [Kubernetes](http://kubernetes.io), [the optional kubernetes extensions defined in OpenShift Origin]() and [Docker](http://docker.io/). (For non-linux platforms which don't support docker we have a [Docker Emulation Layer](emulation.html))

Because of this approach we do quite a few things a little differently at the implementation layer. In fabric8 1.x we had to do a lot of work to mimick the kinds of things kubernetes gives us; things like discovery of services, wiring, auto   scaling etc.

Here are the main differences:

### Service Discovery

Instead of using a ZooKeeper registry in 1.x to store all service endpoints so that we could inject them into other services; we instead use the [Services]() abstraction in kubernetes. This means clients of a network service just bind to a fixed port on the local network; there's no need to have any complex wiring inside the application.

This radically simplifies applications. For example any application using ActiveMQ just needs to bind to the localhost and port of the region of the ActiveMQ cluster. For many Camel endpoints, the endpoint just needs to choose which port number maps to the service it needs to bind to for things like databases.


### Profiles versus Application Templates (Apps)

In fabric8 1.x we focused on profiles in a wiki which are used to represent configuration and metadata which provides a mechanism to build profiles into a container.

In fabric8 2.x we default to using [Kubernetes Application Templates]() to define the kubernetes JSON metadata ([Pods](pods.html), [Replication Controllers](replicationControllers.html), [Services](services.md)).

In addition to support the ability to modify source or metadata inside a wiki / git repository (e.g. camel routes or Java code) we default to using the [Kubernetes source to image extension](). i.e. Profiles map to a build which has a git repository; this can be viewed/editted via the wiki, a web based IDE or traditional IDE or editor.

So source projects are editted; they result in builds which then generate container images which are then used by deployments.


### Auto Scaling

In fabric8 1.x we used requirements we specified on a Profile basis. In 2.x we use [Kubernetes Replication Controller definitions in JSON](replicationControllers.html) to specify how many instances of a certain pod (container) we need to run.

