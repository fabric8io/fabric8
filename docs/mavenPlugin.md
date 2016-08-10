## Fabric8 Maven Plugin

The fabric8 maven plugin makes it easy to work with Docker and Kubernetes or OpenShift from inside your existing Maven project.

### Version 3.x

If you are starting a new project we highly recommend using the new 3.x version of the fabric8-maven-plugin which has the following features:

* much simpler to use!
* can detect most common Java apps and just do the right thing OOTB
* configure via maven XML or by writing optional partial kubernetes YAML files in `src/main/fabric8`
* pluggable generators and enrichers for different kinds of apps (e.g. to auto detect Spring Boot, executable jars, WARs, Karaf etc). 

See the [fabric8 maven plugin 3.x documentation](https://maven.fabric8.io/) for more details!
