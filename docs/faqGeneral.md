### General Questions

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?

fabric8 is an integration platform based on Apache ActiveMQ, Apache Camel, Apache CXF, Hawtio and others.

It provides automated configuration and deployment management to help make deployments easy, reproducible, and less human-error prone.

Take a look [at this blog post](http://www.christianposta.com/blog/?p=376) to see a more detailed treatment.

#### What does fabric8 do?

fabric8 (pronounced _fabricate_) lets you create and manage fabrics (or clusters) of applications, integrations and middleware.

Try reading the [overview](/guide/overview.html) to see if that helps give you an idea what fabric8 is.

#### What value does fabric8 add over OpenShift?

* [Kubernetes](http://kubernetes.io) provides a [Docker](http://docker.io/) based _Container As A Service_ (orchestrates docker containers)
* [OpenShift V3](https://github.com/openshift/origin) extends Kubernetes to support a full _Platform As A Service_
  * hosting source code in git repositories
  * performing builds and hosting private docker images
  * supporting the git-push style model of kicking off new builds
* Fabric8 is focused on:
  * providing a great [Kubernetes Console](console.html) based on [hawtio](http://hawt.io/) so you can view all of the kubernetes resources, understand whats going on and see the big picture or zoom inside inside individual containers and link to logs and metrics
  * being an **Integration Platform As A Service** and a **Java Application Platform As A Service**
  * adding extra services, tooling &amp; quickstarts to make the Kubernetes platform richer and more powerful for the Java ecosystem via:
    * [Fabric8 Apps](fabric8Apps.html) to provide reusable [logging](logging.html), [metrics](metrics.html) and make it easier to consume integration and messaging services
    * deep and rich [tooling](http://fabric8.io/guide/tools.html) to make it easy to develop Java applications on Kubernetes/OpenShift such as the [Maven Plugin](http://fabric8.io/guide/mavenPlugin.html) and [Forge Addons](http://fabric8.io/guide/forge.html)
    * [Java libraries](javaLibraries.html) for working with [kubernetes](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) and [docker](https://github.com/fabric8io/fabric8/tree/master/components/docker-api) along with [kubernetes and jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) so its easier to develop Java based tools and services which work well with Kubernetes
    * [Testing with Arquillian](testing.html) helps you perform integration tests of your [apps](apps.html)
    * making JBoss Middleware reusable appliances with lots of tooling so they are easy to consume in a universal console and platform
    * to support non-Linux platforms which do not yet have native Go Lang or Docker support there's also [Jube](jube.html) which is a pure Java implementation of Kubernetes and emulator of Docker for running Java middleware on any operating system that supports Java 7.

#### What Java versions are supported?

fabric8 runs on Java 7 and 8.

#### Are Docker and Kubernetes required to run Fabric8?

Fabric8 is designed to work best on top of Kubernetes and Docker; it means fabric8 will work very well in any environment providing the Kubernetes platform such as RHEL Atomic, OpenShift, Google Compute Engine, Azure etc.

However we need to be able to work on platforms which don't support Docker; or where you wish to run Java processes directly on the underlying operating system rather than inside docker containers. For those platforms we have [Jube](http://fabric8.io/jube/goals.html) which is a pure Java implementation of Kubernetes which emulates Docker.

#### Is Windows supported

We recommend using a linux based system for production; preferably if you want a fully managed platform use [Docker](http://docker.io/) and [Kubernetes](http://kubernetes.io) or [OpenShift Origin V3](https://github.com/openshift/origin).

Windows is currently only partially supported. Windows users may consider using [Docker](http://docker.io/) so that all the fabric8 technologies run inside a linux VM in lightweight containers.

If you need to run Java on real Windows processes then we recommend using [Jube](http://fabric8.io/guide/jube.html) which will run your Java containers as real windows processes.

#### Does Fabric8 use ZooKeeper runtime registry?

No, not anymore. Fabric8 1.x used ZooKeeper to share the runtime information between applications and to discover services. Kubernetes comes with the [etcd](https://github.com/coreos/etcd) internally which serves much of the same purpose and has support for the services binding, so Fabric8 v2 doesn't need ZooKeeper registry anymore for general purpose provisioning of containers and services.

However certain services will still require master slave election and partitioning functionality (such as running clusters of ActiveMQ); where either [etcd](https://github.com/coreos/etcd) or [Apache ZooKeeper](http://zookeeper.apache.org/) is required. If a Kubernetes environment allows it then fabric8 could reuse the underlying etcd cluster; otherwise an etcd or ZK clusters is required for things like ActiveMQ clustering.

#### Does Fabric8 still use profiles to configure application deployment?

No, not anymore. Starting from v2 Fabric8 uses [app](http://fabric8.io/guide/apps.html) JSON files (i.e. Kubernetes extension proposed by OpenShift 3)
to configure deployment of the managed application. More detailed configuration (like properties or YAML files) can be 
added to the file system of the application's Docker image.

#### Is Git repository still used to store the applications' configuration?

Applications' configuration isn't stored in Git repository as it used to be in Fabric8 v1. As Fabric8 v2 doesn't use 
profiles (but app templates instead), the Git repository is not needed anymore. You can just store application's configuration (app file)
in the Maven project and use the [Fabric8 Maven plugin to start](http://fabric8.io/guide/mavenPlugin.html#running) the application in Kubernetes
without keeping the configuration in any central repository (like Git).

However keeping app files in Git for easier configuration management can be useful. That's why Hawt.io provides this
functionality for you. You can push your configuration to the Hawt.io Git repository via 
[fabric8:deploy](http://fabric8.io/guide/mavenPlugin.html#deploying) Maven goal. Fabric8 uses [App Zip](http://fabric8.io/guide/appzip.html)
packaging format to distribute the configuration between the various environments.

#### Is Fabric8 server required to provision applications?

No, not anymore. Starting from Fabric8 v2 Kubernetes is responsible for providing the runtime registry for the
managed applications. It means that you don't have to start any dedicated Fabric8 deamon. Tools like [Fabric8 Maven plugin](http://fabric8.io/guide/mavenPlugin.html)
or [Hawt.io](http://hawt.io) can connect directly to the Kubernetes and deploy/manage it.

If you are using [Jube as the Kubernetes implementation](http://fabric8.io/jube/getStarted.html) and to emulate Docker then you will need to run a Jube server.

#### If there is no Fabric8 server, how can I use Fabric8 shell?

For the Fabric8 v2 development activities, the recommended tool is [JBoss Forge](http://forge.jboss.org) with the [Fabric8 add-on](forge.html).

For provisioning purposes (like creating containers/services or changing the replica sizes) you can use the [Fabric8 add-on](forge.html) or use the shell from OpenShift/Kubernetes.

Kubernetes will be included in [Red Hat Enterprise Linux](http://www.redhat.com/en/technologies/linux-platforms/enterprise-linux) and OpenShift V3 and is the standard shell for provisioning any kind of the container.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.2 release and removed from 2.x.

