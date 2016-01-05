### General Questions

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?

Fabric8 is an integrated open source [DevOps](fabric8DevOps.html) and [Integration Platform](ipaas.html) which works out of the box on any [Kubernetes](http://kubernetes.io/) or [OpenShift](http://www.openshift.org/) environment and provides [Continuous Delivery](cdelivery.html), [Management](management.html), [ChatOps](chat.html) and a [Chaos Monkey](chaosMonkey.html).



#### What does fabric8 do?

Fabric8 (pronounced _fabricate_) gives you out of the box services that assist you when building microservices, monoliths or any application in a linux container (Docker/Rocket) environment and is built on top of [Kubernetes](http://kubernetes.io/). 

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
    * [Java libraries](javaLibraries.html) for working with [kubernetes](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) along with [kubernetes and jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) so its easier to develop Java based tools and services which work well with Kubernetes
    * [Testing with Arquillian](testing.html) helps you perform integration tests of your [apps](apps.html)
    * making JBoss Middleware reusable appliances with lots of tooling so they are easy to consume in a universal console and platform

#### Is Fabric8 Java centric?

The short answer is no ;).

* [Fabric8 Management](management.html) works with any Docker images on [Kubernetes](http://kubernetes.io/) - so its completely language, framework and runtime agnostic. The [console](console.html) has added extra introspection and visualisation for Java docker containers which contain a [Jolokia](http://jolokia.org/) but we hope to add more deep introspection tools for other languages. Certainly you can always use any language, framework or runtime specific diagnostic or visualiation tools on Kubernetes directly
* [Fabric8 DevOps](fabric8DevOps.html) focusses on working with any project with any build mechanism running Docker images on [Kubernetes](http://kubernetes.io/). So any language, framework and runtime is supported with any build tooling. Whether thats using OpenShift's [Source to image build mechanism](https://docs.openshift.com/enterprise/3.0/architecture/core_concepts/builds_and_image_streams.html#source-build), or our preferred [Continuous Delivery](cdelivery.html) using [Jenkins Workflow](https://github.com/jenkinsci/workflow-plugin). 
* [Fabric8 iPaaS (Integration Platform](ipaas.html) is more Java centric in the sense that integration flows tend to be implemented using [Apache Camel](http://camel.apache.org/) which runs inside a Java virtual machine (JVM) but the services that Camel integrates with can be any technology, language, runtime, on premise or SaaS etc.
* [Fabric8 API Management](apiManagement.html) works with any API implemented in any language or runtime; currently only HTTP based APIs are supported though.

Having said all that; with the  [Fabric8 iPaaS)](ipaas.html) focus, we have optimised Fabric8 so that folks who do use Java have an optimised experience of working with Docker, Kubernetes, OpenShift and Fabric8. Though we hope to continue to improve tooling, management and visualisation for other languages and runtimes too.

#### Is Jenkins Workflow Java centric?

Our preferred tool for [Continuous Delivery](cdelivery.html) is to use the [Jenkins Workflow plugin](https://github.com/jenkinsci/workflow-plugin) with [Jenkins](https://jenkins-ci.org/).

Jenkins Workflow provides a domain specific language for orchestrating long running build tasks such as building, testing, approving, promoting and deploying steps using the [Groovy programming language](http://groovy-lang.org/).

The preferred approach to using Jenkins Workflow is to reuse docker images for all your build and testing tools; so that most of the details in your **Jenkinsfile** tends to be running commands inside docker images. So your CD pipeline definition is usually a list of commands using whatever tools you use (Maven, Grunt, Gulp, Make, bash, python, ruby, whatever) which is completely language, tool and framework agnostic.

#### Can Fabric8 DevOps work with my CI server?

Our preferred tool for [Continuous Delivery](cdelivery.html) is to use the [Jenkins Workflow plugin](https://github.com/jenkinsci/workflow-plugin) with [Jenkins](https://jenkins-ci.org/).

However this is for _orchestrating delivery pipelines_ which typically involves many tasks such as building, testing, approving, promoting and deploying. How each of those parts work is completely up to you.
 
For example you can reuse your existing CI server (Jenkins, Bamboo, TeamCity or whatever) to build your code then use a Jenkins Workflow pipeline to move the build through environments, orchestrate system tests, soak tests, acceptance tests, approvals, promotions and so forth.

Our preferred approach is to use Jenkins Workflow pipelines as the core orchestration layer when trying to implement  [Continuous Delivery, Continous Deployment or Continous Improvement](cdelivery.html) then for that pipeline to trigger whatever is required to complete the pipeline; whether its one or more builds in an existing CI server, triggering OpenShift [Source to Image builds](https://docs.openshift.com/enterprise/3.0/architecture/core_concepts/builds_and_image_streams.html#source-build) or other existing build or test services then orchestrating those along with approval and promotion through Jenkins workflow. It also then means its easier to get a holistic view of your CD pipelines across all projects; irrespective of how each build or test works or what tools are used to build or test projects etc.
 
#### Where do I look for the source code?

Fabric8 is comprised of a collection of projects written in Java and Golang and packaged up as Docker containers. The git repos for each of these projects/containers can be found in detail in the [project documentation pages](projects.md) 

#### Are Docker and Kubernetes required to run Fabric8?

Fabric8 is designed to work best on top of Kubernetes and Docker; it means fabric8 will work very well in any environment providing the Kubernetes platform such as RHEL Atomic, OpenShift, Google Compute Engine, Azure etc.

#### Is Windows supported

We recommend using a linux based system for production; preferably if you want a fully managed platform use [Docker](http://docker.io/) and [Kubernetes](http://kubernetes.io) or [OpenShift Origin V3](https://github.com/openshift/origin).

Windows is currently only partially supported. Windows users may consider using [Docker](http://docker.io/) so that all the fabric8 technologies run inside a linux VM in lightweight containers.

#### What maven plugin goals are available?
 
See the [list of maven plugin goals](http://fabric8.io/guide/mavenPlugin.html) 

#### What Java versions are supported?

fabric8's Java code uses Java 8 but any docker image can use any version of any language, runtime or framework it wishes

#### Does Fabric8 use ZooKeeper runtime registry?

No, not anymore. Fabric8 1.x used ZooKeeper to share the runtime information between applications and to discover services. Kubernetes comes with the [etcd](https://github.com/coreos/etcd) internally which serves much of the same purpose and has support for the services binding, so Fabric8 v2 doesn't need ZooKeeper registry anymore for general purpose provisioning of containers and services.

However certain services will still require master slave election and partitioning functionality (such as running clusters of ActiveMQ); where either [etcd](https://github.com/coreos/etcd) or [Apache ZooKeeper](http://zookeeper.apache.org/) is required. If a Kubernetes environment allows it then fabric8 could reuse the underlying etcd cluster; otherwise an etcd or ZK clusters is required for things like ActiveMQ clustering.

#### Does Fabric8 still use profiles to configure application deployment?

No, not anymore. Starting from v2 Fabric8 uses [app](apps.html) JSON files (i.e. Kubernetes extension proposed by OpenShift 3)
to configure deployment of the managed application. More detailed configuration (like properties or YAML files) can be 
added to the file system of the application's Docker image.

#### Is Git repository still used to store the applications' configuration?

Applications' configuration isn't stored in Git repository as it used to be in Fabric8 v1. As Fabric8 v2 doesn't use 
profiles (but app templates instead), the Git repository is not needed anymore. You can just store application's configuration (app file)
in the Maven project and use the [Fabric8 Maven plugin to start](mavenPlugin.html#running) the application in Kubernetes
without keeping the configuration in any central repository (like Git).

However keeping app files in Git for easier configuration management is recommended. That's why fabric8 has integrated with [Gogs](http://gogs.io/) for on premise git repository hosting.  This comes from the [fabric8 DevOps](fabric8DevOps.md) features.

#### Is Fabric8 server required to provision applications?

No, not anymore. Starting from Fabric8 v2 Kubernetes is responsible for providing the runtime registry for the
managed applications. It means that you don't have to start any dedicated Fabric8 deamon. Tools like 
[Fabric8 Maven plugin](mavenPlugin.html)
or [Hawt.io](http://hawt.io) can connect directly to the Kubernetes and deploy/manage it.

#### If there is no Fabric8 server, how can I use Fabric8 shell?

For the Fabric8 v2 development activities, the recommended tool is [JBoss Forge](http://forge.jboss.org) with the [Fabric8 add-on](forge.html).

For provisioning purposes (like creating containers/services or changing the replica sizes) you can use the [Fabric8 add-on](forge.html) or use the shell from OpenShift/Kubernetes.

Kubernetes will be included in [Red Hat Enterprise Linux](http://www.redhat.com/en/technologies/linux-platforms/enterprise-linux) and OpenShift V3 and is the standard shell for provisioning any kind of the container.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.2 release and removed from 2.x.

