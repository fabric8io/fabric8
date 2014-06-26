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