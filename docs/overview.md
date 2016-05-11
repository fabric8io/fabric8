## Overview

<b>fabric8</b> is an opinionated open source [Microservices Platform](fabric8DevOps.html)  based on <a href="http://docker.com/">Docker</a>, <a href="http://kubernetes.io/">Kubernetes</a> and <a href="https://jenkins.io/">Jenkins</a>

<b>fabric8</b> makes it easy to create microservices, build, test and deploy them via <a href="http://fabric8.io/guide/cdelivery.html">Continuous Delivery pipelines</a> then <a href="http://fabric8.io/guide/fabric8DevOps.html">run and manage them</a> with Continuous Improvement and <a href="http://fabric8.io/guide/chat.html">ChatOps</a>

The [Fabric8 Microservices Platform](fabric8DevOps.html) provides:

* [Developer Console](console.html) is a rich web application which provides a single plane of glass to create, edit, build, deploy and test microservices
* [Continuous Integration and Continous Delivery](http://fabric8.io/guide/cdelivery.html) to help you deliver software faster and more
  reliably using [Jenkins](https://jenkins.io/) with a
  [Jenkins Workflow Library](jenkinsWorkflowLibrary.html) for reusable
  CD pipelines 

* [Management](http://fabric8.io/guide/management.html) of your
  applications with centralised
  [Logging](http://fabric8.io/guide/logging.html) and 
  [Metrics](http://fabric8.io/guide/metrics.html), [ChatOps](http://fabric8.io/guide/chat.html) 
  and [Chaos Monkey](http://fabric8.io/guide/chaosMonkey.html) along with deep
  management of Java Containers using [Hawtio](http://hawt.io/) and
  [Jolokia](http://jolokia.org/)
  

* [Integration](ipaas.html) *Integration Platform As A
Service* with [deep visualisation](http://fabric8.io/guide/console.html) of your
[Apache Camel](http://camel.apache.org/) integration services, an
[API Registry](http://fabric8.io/guide/apiRegistry.html) to view of
all your RESTful and SOAP APIs and
[Fabric8 MQ](http://fabric8.io/guide/fabric8MQ.html) provides
*Messaging As A Service* based on
[Apache ActiveMQ](http://activemq.apache.org/)

* [Java Tools](http://fabric8.io/guide/tools.html) helps the
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
