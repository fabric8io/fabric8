## Overview

*fabric8* is an open source
[DevOps](http://fabric8.io/guide/fabric8DevOps.html) and
[Integration Platform](http://fabric8.io/guide/ipaas.html) which is
built as a reusable set of microservices that run on top of
[Kubernetes](http://kubernetes.io/) and
[OpenShift V3](http://www.openshift.org/)


### [Fabric8 DevOps](fabric8DevOps.html)

[Fabric8 DevOps](fabric8DevOps.html) provides:

* [Continuous](http://fabric8.io/guide/cdelivery.html) Integration and
  Continous Delivery to help you deliver software faster and more
  reliably using [Jenkins](https://jenkins-ci.org/) with a
  [Jenkins Workflow Library](jenkinsWorkflowLibrary.html) for reusable
  CD pipelines with integrated
  [Chat](http://fabric8.io/guide/chat.html) and
  [Chaos Monkey](http://fabric8.io/guide/chaosMonkey.html)

* [Management](http://fabric8.io/guide/management.html) of your
  applications with a powerful
  [Console](http://fabric8.io/guide/console.html) with centralised
  [Logging](http://fabric8.io/guide/logging.html) and
  [Metrics](http://fabric8.io/guide/metrics.html) along with deep
  management of Java Containers using [Hawtio](http://hawt.io/) and
  [Jolokia](http://jolokia.org/)

### [Fabric8 iPaaS](ipaas.html)

[Fabric8 iPaaS](ipaas.html) is an *Integration Platform As A
Service* with
[deep visualisation](http://fabric8.io/guide/console.html) of your
[Apache Camel](http://camel.apache.org/) integration services, an
[API Registry](http://fabric8.io/guide/apiRegistry.html) to view of
all your RESTful and SOAP APIs and
[Fabric8 MQ](http://fabric8.io/guide/fabric8MQ.html) provides
*Messaging As A Service* based on
[Apache ActiveMQ](http://activemq.apache.org/)

### [Fabric8 Tools](tools.html)

[Fabric8 Tools](http://fabric8.io/guide/tools.html) helps the
Java community take full advantage of
[Kubernetes](http://kubernetes.io/):

* [Maven Plugin](http://fabric8.io/guide/mavenPlugin.html) for working
  with [Kubernetes](http://kubernetes.io/)
* [Integration and System Testing](http://fabric8.io/guide/testing.html)
  of [Kubernetes](http://kubernetes.io/) resources easily inside
  [JUnit](http://junit.org/) with [Arquillian](http://arquillian.org/)
* [Java Libraries](http://fabric8.io/guide/javaLibraries.html) and
  support for [CDI](http://fabric8.io/guide/cdi.html) extensions for
  working with [Kubernetes](http://kubernetes.io/)

### Supported Platforms

Fabric8 works great with [Docker](http://www.docker.com/) and
implementations of [Kubernetes](http://kubernetes.io/) such as
[Kubernetes itself](http://kubernetes.io/),
[OpenShift V3](http://openshift.github.io/),
[Project Atomic](http://www.projectatomic.io/) and
[Google Container Engine](https://cloud.google.com/container-engine/).

Kubernetes is supported on Google and Microsofts clouds, by OpenShift
V3 (on premise and public cloud), by Project Atomic and VMware; so
it's increasingly becoming the standard API to PaaS and _Container As
A Service_ on the open hybrid clouds.
