
### 2.4.24

* We now release package metadata with the fabric8-platform so that we can use the [gofabric8 tool to upgrade and downgrade releases easily](http://fabric8.io/guide/getStarted/gofabric8.html#upgrading-fabric8) via `gofabric8 upgrade` along with `gofabric8 packages` and `gofabric8 package-versions`

### 2.4.21

* Jenkins Pipelines now use [ImageStreams](https://docs.openshift.com/container-platform/3.4/architecture/core_concepts/builds_and_image_streams.html) when deploying to different environments on OpenShift
* Jenkins master will run as a non privileged container

### 2.4.19

* Jenkins pipelines will now detect if running on OpenShift for non java application pipelines and avoid mounting the docker socket in build pods, handing off to [OpenShift binary source](https://docs.openshift.com/enterprise/3.2/dev_guide/builds.html#binary-source) to build docker images.
* Pipeline start time improvements

### 2.4.18

* fix to fabric8-maven-plugin to avoid chmod'ing non persistent volume mounts https://github.com/fabric8io/fabric8-maven-plugin/issues/772


### 2.4.15

This release fixes a number of gremlins using fabric8 with kubernetes 1.5.x and the latest minikube

### 2.4.5

* to make it easier to keep track of which fabric8 platform release version you are using [gofabric8](https://github.com/fabric8io/gofabric8/releases) now clearly outputs the platform version when you [install fabric8](http://fabric8.io/guide/getStarted/gofabric8.html)
* we now default to use the internal git URL for gogs repositories (http://gogs/...) when creating projects and building them. This works around all kinds of DNS issues folks have seen; particularly when using the public cloud since the pods inside kubernetes now no longer need to be able to see the public DNS names to navigate into gogs.
* support for [installing, upgrading and removing fabric8 via helm](http://fabric8.io/guide/helm.html)
* on kubernetes we now support specifying a branch on the build settings form

### 2.4.1

* Upgrade to Jenkins2.x alpine base image
* Include the very cool Jenkins [Blue Ocean](https://jenkins.io/projects/blueocean/)
* Use the new @Library to import [fabric8 Jenkins CPS Shared library](https://github.com/fabric8io/fabric8-pipeline-library) in Jenkinsfiles  
* Add experimental Keycloak app
* Upgrade experimental Taiga 3.0.0 and add persistence
* Define multiple containers to include in a build pod from a Jenkinsfile.  [Excellent blog](https://blog.fabric8.io/jenkins-kubernetes-plugin-adds-pipeline-capabilities-2d43f934c580#.iaht2qp1y) by Ioannis

### 2.3.19

* Adds support for persistence to various apps (gogs, nexus, jenkins). To create persistent volumes or opt out of persistence check the [persistence guide](persistence.html)  
* Includes documentation on [how to create Spring Boot microservices on kubernetes with fabric8](https://spring.fabric8.io/)

### 2.2.234

* We now use the [exposecontroller](https://github.com/fabric8io/exposecontroller/) to decide whether to create Ingress resources on Kubernetes, Route resources on OpenShift, use external IPs via LoadBalancer types on public cloud or use nodePorts on local single node installations.      
* All quickstarts and archetypes have moved to the new 3.x version of the [fabric8 maven plugin](https://maven.fabric8.io/)

### 2.2.16

* [Fabric8 DevOps](http://fabric8.io/guide/fabric8DevOps.html) has new [chat based](http://fabric8.io/guide/chat.html) [workflow steps](https://github.com/fabric8io/fabric8-jenkins-workflow-steps) for notification and approval of Jenkins Workflow based [Continuous Deployment pipelines](http://fabric8.io/guide/cdelivery.html) inside the chat application like Lets Chat or Slack. Here's [an example so you can see how it looks](https://github.com/fabric8io/fabric8-jenkins-workflow-steps#hubotapprove) using the [Fabric8 DevOps library of reusable Jenkins Workflows](https://github.com/fabric8io/jenkins-workflow-library)
* All generated [Kuberentes integration tests](http://fabric8.io/guide/testing.html) are now invoked by default in the [Fabric8 DevOps reusable Jenkins Workflows](https://github.com/fabric8io/jenkins-workflow-library)
* Fixes [these 10 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.16)

* **Note** we highly recommend all fabric8 users to upgrade to this release ASAP. This release fixes [a bug](https://github.com/fabric8io/fabric8/issues/4520) which showed up in recent versions of Chrome where we used the wrong URL during OpenShift OAuth login causing folks not to be able to login with the [console](http://fabric8.io/guide/console.html)  unless explicitly trusting the certificate.

### 2.2.14

* This release fixes some regressions found in OpenShift 1.0.2 or later when using [Continuous Deliver with Fabric8 DevOps](http://fabric8.io/guide/cdelivery.html)
* Adds nicer links to [Continuous Deployment environments](http://fabric8.io/guide/cdelivery.html) in the [console](http://fabric8.io/guide/console.html)
* Fixes [these 2 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.14)

### 2.2.12

* Upgraded to the latest kubernetes 1.0 schema. This version now works on OpenShift Origin 1.0.2 or later
* Fully working Canary release, stage, approve and promote builds in the Jenkins Workflow when using [Continuous Deliver with Fabric8 DevOps](http://fabric8.io/guide/cdelivery.html)
* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.12)

### 2.2.11

* This release now means that the [fabric8 console](http://fabric8.io/guide/console.html) works [great on vanilla kubernetes and GKE](http://fabric8.io/guide/getStarted/gke.html) as well as OpenShift. Check out this [demo video](https://vimeo.com/133765913) to see it on GKE
* The [mvn fabric8:apply](http://fabric8.io/guide/mavenFabric8Apply.html) goal now works out of the box on both vanilla Kubernetes and OpenShift without you having to configure any properties
* A new [mvn fabric8:recreate](http://fabric8.io/guide/mavenFabric8Recreate.html) goal for less typing if you want to ensure all resources are created or recreated
* Fixes [these 18 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.11)

### 2.2.7

* first release of the new [Jenkins Docker Workflow based](http://documentation.cloudbees.com/docs/cje-user-guide/docker-workflow.html) [Continuus Integration / Continuous Delivery system](http://fabric8.io/guide/cdelivery.html)
* integrated support for the [fabric8-devops-connector](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-devops-connector) to connect various DevOps services like git hosting, chat, issue tracking and jenkins for a project reusing the optional `fabric8.yml` file via JBoss Forge, Maven or in a JVM.
* The `fabric8:create-build-config` goal is now renamed to `fabric8:devops` to reflect the more generic nature of updating the DevOps configuration via the [fabric8-devops-connector](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-devops-connector)
* Fixes [these 16 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.7)

### 2.2.5

* The new project wizard now lets you configure a [fabric8.yml file](https://github.com/fabric8io/fabric8/issues/4086) like this [example](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-devops/src/test/resources/fabric8.yml) for configuring the devops side of a project such as the chat room and issue tracker for a project and whether code review is enabled. Over time when we move to the [Jenkins Docker Workflow](https://github.com/fabric8io/fabric8/issues/4286) for builds; we'll be able to use configured the flow too.
* Fixes [these 13 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.5)

### 2.2.3

* The [Metrics](http://fabric8.io/guide/metrics.html) and [Logging](http://fabric8.io/guide/chat.html) now work out of the box if you run them by pressing the `Run...` button on the `Apps` tab in the [console](http://fabric8.io/guide/console.html) when using the [vagrant image](http://fabric8.io/guide/getStartedVagrant.html)
* The [Chat](http://fabric8.io/guide/chat.html) now works out of the box without having to manually figure out rooms and tokens and pass them on the command line so that Hubot can connect to Let's Chat
* Fixes [these 3 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.3)

### 2.2.2

* New getting started guide based on a new easier to use [vagrant image](http://fabric8.io/guide/getStartedVagrant.html)
* The vagrant domain is now `vagrant.f8` getting ready for DNS support inside the vagrant image
* You can now easily run the [fabric8 apps](http://fabric8.io/guide/fabric8Apps.html) from the `Run...` button on the [console](http://fabric8.io/guide/console.html)
* Fixes [these 39 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.2)

### 2.2.0

* Updates to the `v1` schema version in kubernetes
* The [fabric8 vagrant image](http://fabric8.io/guide/openShiftWithFabric8Vagrant.html) has moved into the [fabric8-installer](https://github.com/fabric8io/fabric8-installer/tree/master/vagrant/openshift-latest) repository
* Fixes [these 15 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.2.0)

### 2.1.11

* Fixes [these 3 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.1.11)

### 2.1.10

* Fixes [these 8 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.1.10)

### 2.1.6

* Fixes [these 39 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.1.6)

### 2.1.1

* Improved [user guide](http://fabric8.io/guide/) which is hopefully more clear along with instructions on how to [Install Fabric8 on OpenShift V3](http://fabric8.io/guide/fabric8OnOpenShift.html)
* migrates to the v1beta3 version of the Kubernetes schema by default including much better validation
* [Fabric8 Apps](http://fabric8.io/guide/fabric8Apps.html) are now easier to [install on an existing Kubernetes or OpenShift environment](http://fabric8.io/guide/fabric8OnOpenShift.html) via a [OpenShift templates](http://docs.openshift.org/latest/dev_guide/templates.html) JSON file.
* Maven [fabric8:json](http://fabric8.io/guide/mavenFabric8Json.html) goal now supports the generation of [OpenShift templates](http://docs.openshift.org/latest/dev_guide/templates.html)
* Maven [fabric8:apply](http://fabric8.io/guide/mavenFabric8Apply.html) goal is the new name of the old `fabric8:run` goal to better describe applying JSON to a kubernetes environment and creating/updating/deleting resources.
* New maven [fabric8:create-routes](http://fabric8.io/guide/mavenFabric8CreateRoutes.html) to lazily create any missing [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html)
* The [fabric8 console](http://fabric8.io/guide/console.html) is now more reactive thanks to the support of websockets for real time updates
* First spike of [Fabric8 Continuous Delivery](http://fabric8.io/guide/cdelivery.html) making it easier to build, release and provision software faster and more reliably
* Fixes [these 49 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.1.1)

### 2.0.44

* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.44)

### 2.0.43

* Fixes [these 2 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.43)

### 2.0.40

* Includes [Chat support](http://fabric8.io/guide/chat.html) via [hubot](https://hubot.github.com/) for notifying chat rooms in IRC, Slack, HipChat or Campfire on build completion or failure etc.
* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.40)

### 2.0.36

* Fixes [these 20 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.36)

### 2.0.32

* Fixes [these 60 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.32)

### 2.0.29

* Migrated to hawtio 2.x for the console making it small and lightweight with [optional modular services](http://fabric8.io/guide/fabric8Apps.html)
* Added the [App Library](appLibrary.html) to provide a configurable library of Apps you can easily run and install or uninstall; rather like your library of Apps on a mobile device.
* Fixes [these 18 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.29+is%3Aclosed)

### 2.0.26

* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.26+is%3Aclosed)

### 2.0.25

* Handle the new HTTPS only REST API in Kubernetes / OpenShift
* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.25+is%3Aclosed)

### 2.0.19

* Uses auto-generated JSON Schema from the OpenShift/Kubernetes go source code to provide faithful Jackson DTOs for the REST API against v1beta2 of the kubernetes/openshift APIs
* Improved App tab in the console showing a nicer detailed view of apps/pods
* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.19+is%3Aclosed)

### 2.0.18

* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.18+is%3Aclosed)

### 2.0.17

* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.17+is%3Aclosed)

### 2.0.15

* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.15+is%3Aclosed)

### 2.0.14

* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.14+is%3Aclosed)

### 2.0.12

* Added a quickstart for camel-sql which uses the Camel REST API and shows up in the API Registry / API console
* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.12+is%3Aclosed)

### 2.0.11

* Much nicer console which now has
  * deep linking of Pods to Kibana logs if kibana is running
  * allows you to view the number of pods and their status on the Controllers and Services pages
  * links on the Controllers and Services pages to the pods running for a single Controller or Service
* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.11+is%3Aclosed)

### 2.0.10

* Fixes [these 12 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.10+is%3Aclosed)

### 2.0.9

* Ported the [API console to work on Kubernetes](https://github.com/hawtio/hawtio/issues/1743) so that the APIs tab appears on the [Fabric8 Console](http://fabric8.io/guide/console.html) if you run hawtio inside Kubernetes and are running the [API Registry service](https://github.com/fabric8io/quickstarts/tree/master/apps/api-registry)
* Adds [Service wiring for Kubernetes](https://github.com/hawtio/hawtio/blob/master/docs/Services.md) so that its easy to dynamically link nav bars, buttons and menus to remote services running inside Kubernetes (e.g. to link nicely to Kibana, Grafana etc).
* Fixes [these 10 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.9+is%3Aclosed)

### 2.0.8

* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.8+is%3Aclosed)

### 2.0.7

* Fixes [these 4 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.7+is%3Aclosed)

### 2.0.6

* Fixes [this issue](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.6)

### 2.0.5

* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.5+is%3Aclosed)

### 2.0.2

* Nicer layouts and icons in the [Console](http://fabric8.io/guide/console.html) (and the Wiki tab is now called Library as in an Application Library)
* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.2+is%3Aclosed)

### 2.0.1

* Improve the appearance of [App Zips](http://fabric8.io/guide/appzip.html) when deployed (or dragged and dropped) into the [Console](http://fabric8.io/guide/console.html) Wiki tab
* Fixes [these 5 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.1+is%3Aclosed)

### 2.0.0

* First release of the [Kubernetes based Fabric8](http://fabric8.io/guide/overview.html) which reuses the standard [Kubernetes](http://kubernetes.io/) REST APIs for container orchestration and either [Docker container images](http://docker.com) or [Jube image zips](http://fabric8.io/jube/imageZips.html) for container provisioning.
* Fixes [these 8 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A2.0.0)

### 1.2.0.Beta5

* Profile name must be in lower-case letters

### 1.2.0.Beta4

* Improves startup experience so its a bit more clear when fabric8 has completed provisioning.
* Improvements to the REST API so you can easily view containers for a profile, start/stop/delete containers or POST new profiles to a version or DELETE profiles
* Fixes [these 60 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A1.2.0.Beta4)

Note that until we move to CXF 3.x the REST API is at http://localhost:8181/cxf/fabric8 and not its usual http://localhost:8181/api/fabric8 - you can always use the **fabric:info** CLI command to find it.

### 1.2.0.Beta3

* Fixes running fabric8 inside docker and creating containers from within docker
* Fixes [these 12 issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A1.2.0.Beta3)

### 1.2.0.Beta2

* Upgrade to [Apache Karaf](http://karaf.apache.org/) 2.4.x build so that we get full Role Based Access Control (RBAC) support on all MBeans, OSGi services and CLI commands (hence the upgrade to 1.2.x as the change to RBAC was bigger than a minor fix to 1.1.x)
* Container names must be lower case only
* Improved [Auto Scaling](http://fabric8.io/gitbook/requirements.html) UI for easier configuration of the [Auto Scaling Requirements](http://fabric8.io/gitbook/requirements.html)
* Improved Configuration tab on the Profile page in the web console so its easier to configure all profiles whether containers are running or not and whether they actually use OSGi or not.
* New [Arquillian](http://fabric8.io/gitbook/arquillian.html) integration testing framework for easier testing against remote or docker based fabrics
* Fixes to various things like Java containers, Tomcat and Docker
* Fixes [these issues](https://github.com/fabric8io/fabric8/issues?q=milestone%3A1.2.0.Beta1+is%3Aclosed)

### 1.1.0.CR5

* [AutoScaler](http://fabric8.io/gitbook/requirements.html) can now properly recreate ssh containers if the [ssh hosts are specified in the json](https://github.com/fabric8io/fabric8-devops/blob/master/autoscaler/ssh-mq-demo.json#L29) like in [this example](https://github.com/fabric8io/fabric8-devops/tree/master/autoscaler) plus there is a new **autoscale-status** CLI command to see how the auto scaler is progressing
* Fixes [these 56 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=11&state=closed)

### 1.1.0.CR3

* New top level Profiles tab in the web console makes it nice and easy to view and search all profiles; filtering by text or tag with nice icons and summary text coming from icon.(svg,png,jpg) and Summary.md files in the wiki
* First spike of Fabric DNS support
* [AutoScaler](http://fabric8.io/gitbook/requirements.html) can now properly recreate  [Java Container](http://fabric8.io/gitbook/javaContainer.html) and [Process Container](http://fabric8.io/gitbook/processContainer.html) instances if the process is explicitly killed
* The feature name for the [amq: endpoint](http://fabric8.io/gitbook/camelEndpointAmq.html), mq-fabric-camel has been renamed to camel-amq which is more usual name for camel feature names
* Fixes [these 132 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=10&page=1&state=closed)

### 1.1.0.CR2

* Renamed CLI command `fabric:profile-download` to `fabric:profile-download-artifacts`
* Fixes [these 165 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=7&state=closed)

### 1.1.0.CR1

* Working [Java Container](http://fabric8.io/gitbook/javaContainer.html) for working with [Micro Services](http://fabric8.io/gitbook/microServices.html) and [Spring Boot](http://fabric8.io/gitbook/springBootContainer.html)
* Great [Spring Boot](http://fabric8.io/gitbook/springBootContainer.html) integration
* Support for Apache Tomcat, TomEE and Jetty as web containers
* integration with [fabric:watch *](http://fabric8.io/gitbook/developer.html#rad-workflow) and various containers like  [Java Container](http://fabric8.io/gitbook/javaContainer.html), [Spring Boot](http://fabric8.io/gitbook/springBootContainer.html), Tomcat, TomEE, Jetty
* lots more [QuickStart examples](http://fabric8.io/gitbook/quickstarts.html) which are all included in the [distribution](http://fabric8.io/gitbook/getStarted.html) and turned into archetypes
* new [Continuous Deployment commands](continuousDeployment.md) like _profile-import_, _profile-export_ and improved  [mvn fabric8:zip goal that works with multi-maven projects](http://fabric8.io/gitbook/mavenPlugin.html)
* Fixes [these 136 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=6&state=closed)

### 1.1.0.Beta6

* Fixes [these 176 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=5&state=closed)

### 1.1.0.Beta5

* Fixes [these 113 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=8&state=closed)

### 1.1.0.Beta4

* the script to start a container is now **bin/fabric8** and by default it will create a fabric unless you explicitly configure one of the [fabric8 environment variables](http://fabric8.io/gitbook/environmentVariables.html)
* added new [maven plugin goals](http://fabric8.io/gitbook/mavenPlugin.html) (fabric8:agregate-zip, fabric8:zip, fabric8:branch)
* fully working [docker support](http://fabric8.io/gitbook/docker.html) so using the docker profile you can create containers using docker.
* Fixes [these 300 issues and enhancements](https://github.com/fabric8io/fabric8/issues?milestone=9&state=closed)

### 1.0.0.x

* first community release integrated into the [JBoss Fuse 6.1 product](http://www.jboss.org/products/fuse)
* includes [hawtio](http://hawt.io/) as the console
* uses git for configuration; so all changes are audited and versioned and its easy to revert changes.
