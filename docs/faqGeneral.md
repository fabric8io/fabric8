### General Questions

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?

Fabric8 is an integrated open source [DevOps](fabric8DevOps.html) and [Integration Platform](ipaas.html) which works out of the box on any [Kubernetes](http://kubernetes.io/) or [OpenShift](http://www.openshift.org/) environment and provides [Continuous Delivery](cdelivery.html), [Management](management.html), [ChatOps](chat.html) and a [Chaos Monkey](chaosMonkey.html).

#### What does fabric8 do?

Fabric8 (pronounced _fabricate_) gives you out of the box services that assist you when building Microservices, monoliths or any application in a linux container (Docker/Rocket) environment and is built on top of [Kubernetes](http://kubernetes.io/).

#### What is Kubernetes?

[Kubernetes](http://kubernetes.io) provides a [Docker](http://docker.io/) based _Container As A Service_ (orchestrates docker containers)

#### How does Fabric8 relate to other open source projects?

Fabric8 provides upstream innovation on concepts and features, once validated, make their way into other open source projects such as [OpenShift Origin](https://github.com/openshift/origin), [Kubernetes](http://kubernetes.io) and [Jenkins](https://jenkins.io/) (as examples). Fabric8 contributors work in the various other open source communities to align a unified developer experience.

#### What is the overlap with oc/kubectl/webconsole and fabric8-maven-plugin?

None!

`oc`/`kubectl`/`webconsole` are all great tools that end up operating on the same resources through the Kubernetes/OpenShift API. They can be used at any point to help manage your applications. The [fabric8-maven-plugin](http://maven.fabric8.io) is intended for Java developers developing applications "pre git-commit" on their laptops. It gives Java developers a familiar way of building/deploying their applications as well as logging, debugging, and live-reloading their applications using Kubernetes as an application cloud. Developers can use their favorite IDE which typically has great Maven integration. The sweet spot of Kubernetes/OpenShift is dealing with the build, deploy, and management of applications *after* a git commit/push. The CLI/webconsole is great at working with API resources, import projects from git, or spin up new projects from a template. 

The fabric8-maven-plugin helps Java developers migrate existing Maven projects to Kubernetes/OpenShift as well. We're all [busy Java developers](https://blog.fabric8.io/a-busy-java-developers-guide-to-developing-microservices-on-kubernetes-and-docker-98b7b9816fdf#.gns2q8wle) and although it would be great to learn the ins/outs of kubectl, oc, Docker, Rkt, CNCF, OCI, Kubernetes, etc, the fabric8-maven-plugin helps abstract some of that away so we can be productive using tooling similar to others (any application-server maven plugin, spring-boot plugin, etc. fabric8-maven-plugin feels just like those others)

#### How should we create the Kubernetes/OpenShift manifest files?

However you'd like. The fabric8-maven-plugin does help automatically generate this for you as part of your builds and even attach these resource files as build artifacts so they can be versioned and pushed to a central artifact repository. 

The fabric8-maven-plugin generates the JSON/YAML resource files and can be customized using Maven configurations. See the [documentation for fabric8-maven-plugin for more details](http://maven.fabric8.io). It can also introspect your projects and be a lot smarter about what resources to include (eg, health/liveness checks can automatically be enabled in the Kubernetes resource files if you've got, for example, spring-boot actuator or some other http endpoint that can expose liveness information).

#### What value does fabric8 add over OpenShift?

* [OpenShift V3](https://github.com/openshift/origin) extends Kubernetes to support a full _Platform As A Service_
  * isolation of source code [builds within containers](https://docs.openshift.org/latest/dev_guide/builds.html)
  * providing a [simple console](https://docs.openshift.org/latest/getting_started/developers_console.html) so you can create, view, modify and remove all of your Kubernetes and OpenShift resources
  * ease of use for [creating and deploy from source or docker formatted image](https://docs.openshift.org/latest/getting_started/developers_cli.html#developers-cli-creating-an-application)
  * supporting the [git-push style model](https://docs.openshift.org/latest/dev_guide/builds.html#build-triggers) of kicking off new builds
  * keep base container layers current with [automatic rebuilds on image changes](https://docs.openshift.org/latest/dev_guide/builds.html#build-triggers)
  * integrated application [metrics](https://docs.openshift.org/latest/install_config/cluster_metrics.html) and [logging](https://docs.openshift.org/latest/install_config/aggregate_logging.html)
  * large set of [builder images](https://docs.openshift.org/latest/using_images/s2i_images/index.html) and [quick starts](https://github.com/openshift/origin/tree/master/examples)
  * flexible [deployment models](https://docs.openshift.org/latest/dev_guide/deployments.html): rolling, blue/green and [load balancing for A/B testing](https://docs.openshift.org/latest/dev_guide/routes.html#routes-load-balancing-for-AB-testing)
* Fabric8 is focused on:
  * being an **Integration Platform As A Service** and a **Java Application Platform As A Service**, which encompasses modern Microservices and legacy application architectures
  * adding extra services, focused around Java tooling &amp; quickstarts:
    * [Fabric8 Apps](fabric8Apps.html) to provide JVM specific metrics
    * deep and rich [tooling](http://fabric8.io/guide/tools.html) to make it easy to develop Java applications on Kubernetes/OpenShift such as the [Maven Plugin](http://fabric8.io/guide/mavenPlugin.html) and [Forge Addons](http://fabric8.io/guide/forge.html)
    * [Java libraries](javaLibraries.html) for working with [kubernetes](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) along with [kubernetes and jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) so its easier to develop Java based tools and services which work well with Kubernetes
    * [Testing with Arquillian](testing.html) helps you perform integration tests of your [apps](apps.html)

#### Is Fabric8 Java centric?

The short answer is no ;).

* [Fabric8 Management](management.html) works with any Docker images on [Kubernetes](http://kubernetes.io/) - so its completely language, framework and runtime agnostic. The [console](console.html) has added extra introspection and visualisation for Java docker containers which contain a [Jolokia](http://jolokia.org/) but we hope to add more deep introspection tools for other languages. Certainly you can always use any language, framework or runtime specific diagnostic or visualiation tools on Kubernetes directly
* [Microservices Platform](fabric8DevOps.html) focusses on working with any project with any build mechanism running Docker images on [Kubernetes](http://kubernetes.io/). So any language, framework and runtime is supported with any build tooling. Whether thats using OpenShift's [Source to image build mechanism](https://docs.openshift.com/enterprise/3.0/architecture/core_concepts/builds_and_image_streams.html#source-build), or our preferred [Continuous Delivery](cdelivery.html) using [Jenkins Workflow](https://github.com/jenkinsci/workflow-plugin).
* [Fabric8 iPaaS (Integration Platform](ipaas.html) is more Java centric in the sense that integration flows tend to be implemented using [Apache Camel](http://camel.apache.org/) which runs inside a Java virtual machine (JVM) but the services that Camel integrates with can be any technology, language, runtime, on premise or SaaS etc.
* [Fabric8 API Management](apiManagement.html) works with any API implemented in any language or runtime; currently only HTTP based APIs are supported though.

Having said all that; with the  [Fabric8 iPaaS)](ipaas.html) focus, we have optimised Fabric8 so that folks who do use Java have an optimised experience of working with Docker, Kubernetes, OpenShift and Fabric8. Though we hope to continue to improve tooling, management and visualisation for other languages and runtimes too.

#### Is Jenkins Pipeline Java centric?

Our preferred tool for [Continuous Delivery](cdelivery.html) is to use the [Jenkins Pipeline plugin](https://github.com/jenkinsci/workflow-plugin) with [Jenkins](https://jenkins.io/).

Jenkins Pipeline provides a domain specific language for orchestrating long running build tasks such as building, testing, approving, promoting and deploying steps using the [Groovy programming language](http://groovy-lang.org/).

The preferred approach to using Jenkins Pipeline is to reuse docker images for all your build and testing tools; so that most of the details in your **Jenkinsfile** tends to be running commands inside docker images. So your CD pipeline definition is usually a list of commands using whatever tools you use (Maven, Grunt, Gulp, Make, bash, python, ruby, whatever) which is completely language, tool and framework agnostic.

#### Can Microservices Platform work with my CI server?

Our preferred tool for [Continuous Delivery](cdelivery.html) is to use the [Jenkins Pipeline plugin](https://github.com/jenkinsci/workflow-plugin) with [Jenkins](https://jenkins.io/).

However this is for _orchestrating delivery pipelines_ which typically involves many tasks such as building, testing, approving, promoting and deploying. How each of those parts work is completely up to you.

For example you can reuse your existing CI server (Jenkins, Bamboo, TeamCity or whatever) to build your code then use a Jenkins Pipeline pipeline to move the build through environments, orchestrate system tests, soak tests, acceptance tests, approvals, promotions and so forth.

Our preferred approach is to use Jenkins Pipeline pipelines as the core orchestration layer when trying to implement  [Continuous Delivery, Continous Deployment or Continous Improvement](cdelivery.html) then for that pipeline to trigger whatever is required to complete the pipeline; whether its one or more builds in an existing CI server, triggering OpenShift [Source to Image builds](https://docs.openshift.com/enterprise/3.0/architecture/core_concepts/builds_and_image_streams.html#source-build) or other existing build or test services then orchestrating those along with approval and promotion through Jenkins workflow. It also then means its easier to get a holistic view of your CD pipelines across all projects; irrespective of how each build or test works or what tools are used to build or test projects etc.

#### Where do I look for the source code?

Fabric8 is comprised of a collection of projects written in Java and Golang and packaged up as Docker containers. The git repos for each of these projects/containers can be found in detail in the [project documentation pages](projects.md)

#### Are Docker and Kubernetes required to run Fabric8?

Fabric8 is designed to work best on top of Kubernetes and Docker; it means fabric8 will work very well in any environment providing the Kubernetes platform such as OpenShift, Google Compute Engine, Azure etc.

#### Is Windows supported

We recommend using a linux based system for production; preferably if you want a fully managed platform use [Docker](http://docker.io/) and [Kubernetes](http://kubernetes.io) or [OpenShift Origin V3](https://github.com/openshift/origin).

Windows is currently only partially supported. Windows users may consider using [Docker](http://docker.io/) so that all the fabric8 technologies run inside a linux VM in lightweight containers.


#### How do I configure a HTTP proxy?

If you are behind a corporate firewall then maven builds may fail to download jars from maven central.

This [guide describes how to configure maven to use HTTP proxies](https://maven.apache.org/guides/mini/guide-proxies.html).

To configure a HTTP Proxy in fabric8 open the [fabric8 console(console.html) then:

* click on your `Team` page
* now select the `Runtime` page on the left tab
* select `Secrets` then the `jenkins-maven-settings` secret
* edit this file - its the `~/.m2/settings.xml` used by default on all maven builds in Jenkins - based on the [this document](https://maven.apache.org/guides/mini/guide-proxies.html)
* once you've saved your changes re-run your build in Jenkins

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

No. Kubernetes is responsible for providing the runtime registry for the managed applications. It means that you don't have to start any dedicated Fabric8 software in production.

Tools like [Fabric8 Maven plugin](mavenPlugin.html) or the [fabric8 developer console](console) can connect directly to the Kubernetes and deploy/manage it.

#### If there is no Fabric8 server, how can I use Fabric8 shell?

For the Fabric8 v2 development activities, the recommended tool is [JBoss Forge](http://forge.jboss.org) with the [Fabric8 add-on](forge.html).

For provisioning purposes (like creating containers/services or changing the replica sizes) you can use the [Fabric8 add-on](forge.html) or use the shell from OpenShift/Kubernetes.

Kubernetes will be included in [Red Hat Enterprise Linux](http://www.redhat.com/en/technologies/linux-platforms/enterprise-linux) and OpenShift V3 and is the standard shell for provisioning any kind of the container.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.2 release and removed from 2.x.
