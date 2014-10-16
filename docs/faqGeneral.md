### General Questions

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?

fabric8 is an integration platform based on Apache ActiveMQ, Apache Camel, Apache CXF, Hawtio and others.

It provides automated configuration and deployment management to help make deployments easy, reproducible, and less human-error prone.

Take a look [at this blog post](http://www.christianposta.com/blog/?p=376) to see a more detailed treatment.

#### What does fabric8 do?

fabric8 (pronounced _fabricate_) lets you create and manage fabrics (or clusters) of applications, integrations and middleware.

Try reading the [overview](/gitbook/overview.html) to see if that helps give you an idea what fabric8 is.

#### What value does fabric8 add over OpenShift?

* [Kubernetes](http://kubernetes.io) provides a [Docker](http://docker.io/) Container As A Service layer
* [OpenShift V3](https://github.com/openshift/origin) extends Kubernetes to support a full Platform As A Service
  * hosting source code in git repositories
  * performing builds and hosting private docker images
  * supporting the git-push style model of kicking off new builds
* Fabric8 is focussed on being a **Java Application Platform As A Service** and an **Integration Platform As A Service** via:
  * deep and rich tooling to make it easy to develop Java applications on Kubernetes/OpenShift
  * [hawtio based console](http://hawt.io/) so you can view your entire environment or zoom inside inside any Java container and see exactly whats going on
  * making JBoss Middleware reusable appliances with lots of tooling so they are easy to consume in a universal console and platform

#### What Java versions is supported?

fabric8 runs on Java 7 and 8.

#### Are Docker and Kubernetes required to run Fabric8?

Fabric8 is designed to work best on top of Kubernetes and Docker; it means fabric8 will work very well in any environment providing the Kubernetes platform such as RHEL Atomic, OpenShift, Google Compute Engine, Azure etc.

However we need the [Emulator](http://fabric8.io/v2/emulation.html) to be able to work on platforms which don't support Docker; or where you wish to run Java processes directly on the underlying operating system rather than inside docker containers.

#### Is Windows supported

We recommend using a linux based system for production; preferably if you want a fully managed platform use [Docker](http://docker.io/) and [Kubernetes](http://kubernetes.io) or [OpenShift Origin V3](https://github.com/openshift/origin).

Windows is currently only partially supported. Windows users may consider using [Docker](http://docker.io/) so that all the fabric8 technologies run inside a linux VM in lightweight containers.

If you need to run Java on real Windows processes then we recommend using the [Emulator](http://fabric8.io/v2/emulation.html) when its complete.rs.

#### Does Fabric8 use ZooKeeper runtime registry?

No, not anymore. Fabric8 1.x used ZooKeeper to share the runtime information between applications and to discover services. Kubernetes comes with the [etcd](https://github.com/coreos/etcd) internally which serves much of the same purpose and has support for the services binding, so Fabric8 v2 doesn't need ZooKeeper registry anymore for general purpose provisioning of containers and services.

However certain services will still require master slave election and partitioning functionality (such as running clusters of ActiveMQ); where either [etcd](https://github.com/coreos/etcd) or [Apache ZooKeeper](http://zookeeper.apache.org/) is required. If a Kubernetes environment allows it then fabric8 could reuse the underlying etcd cluster; otherwise an etcd or ZK clusters is required for things like ActiveMQ clustering.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.2 release and removed form 2.x.

