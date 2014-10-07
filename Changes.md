### 1.2.0.Beta5

* Profile name must be in lower-case letters
* Fixed CPU spike when using hawtio and browsing container or profile details
* Switched to using Apache Karaf 2.4.0

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
* Deploying WARs to Tomcat, TomEE, Jetty or WildFly can now have their context path configured via the [webContextPath](http://fabric8.io/gitbook/mavenPlugin.html#property-reference) property in the maven plugin or in the [io.fabric8.web.contextPath.properties file](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/drools/execution.server.profile/io.fabric8.web.contextPath.properties#L2-2) in a profile
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
