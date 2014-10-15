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

#### Are Docker and Kubernetes required to run Fabric8?

Yes. Fabric8 starting from version 2.0 operates on applications via Kubernetes layer. It means that Fabric8 can be used
in any environment providing the Kubernetes platform. It also means that Kubernetes (and therefore Docker) is a requirement
for the Fabric8.

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

#### Is Windows supported

We recommend using a linux based system for production; preferably if you want a fully managed platform use [Docker](http://docker.io/) and [Kubernetes](http://kubernetes.io) or [OpenShift Origin V3](https://github.com/openshift/origin).

Windows is currently only partially supported. Windows users may consider using [Docker](http://docker.io/) so that all the fabric8 technologies run inside a linux VM in lightweight containers.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.2 release and removed form 2.x.

#### Does Fabric8 use ZooKeeper runtime registry?

No, not anymore. Fabric8 1.x used ZooKeeper to share the runtime information between applications and to discover services. Kubernetes
comes with the [etcd](https://github.com/coreos/etcd) and support for the services binding, so Fabric8 v2 doesn't need 
ZooKeeper registry anymore.