## Overview

<strong>fabric8</strong> is an open source <a href="http://fabric8.io/guide/fabric8DevOps.html">DevOps</a> and <a href="http://fabric8.io/guide/ipaas.html">Integration Platform</a> which is built as a reusable set of microservices that run on top of <a href="http://kubernetes.io/">Kubernetes</a> and <a href="http://www.openshift.org/">OpenShift V3</a>


### [Fabric8 DevOps](fabric8DevOps.html)

[Fabric8 DevOps](fabric8DevOps.html) provides:

* <a href="http://fabric8.io/guide/cdelivery.html">Continuous Integration and Continous Delivery</a> to help you deliver software faster and more reliably using <a href="https://jenkins-ci.org/">Jenkins</a> with a [Jenkins Workflow Library](jenkinsWorkflowLibrary.html) for reusable CD pipelines with integrated <a href="http://fabric8.io/guide/chat.html">Chat</a> and <a href="http://fabric8.io/guide/chaosMonkey.html">Chaos Monkey</a>

* <a href="http://fabric8.io/guide/management.html">Management</a> of your applications with a powerful <a href="http://fabric8.io/guide/console.html">Console</a> with centralised <a href="http://fabric8.io/guide/logging.html">Logging</a> and <a href="http://fabric8.io/guide/metrics.html">Metrics</a> along with deep management of Java Containers using <a href="http://hawt.io/">Hawtio</a> and <a href="http://jolokia.org/">Jolokia</a>

### [Fabric8 iPaaS](ipaas.html)

[Fabric8 iPaaS](ipaas.html) is an <i>Integration Platform As A Service</i> with <a href="http://fabric8.io/guide/console.html">deep visualisation</a> of your <a href="http://camel.apache.org/">Apache Camel</a> integration services, an <a href="http://fabric8.io/guide/apiRegistry.html">API Registry</a> to view of all your RESTful &amp; SOAP APIs and <a href="http://fabric8.io/guide/fabric8MQ.html">Fabric8 MQ</a> provides <i>Messaging As A Service</i> based on <a href="http://activemq.apache.org/">Apache ActiveMQ</a>
      
### [Fabric8 Tools](tools.html)

<a href="http://fabric8.io/guide/tools.html">Fabric8 Tools</a></h3> helps the Java community take full advantage of <a href="http://kubernetes.io/">Kubernetes</a>:

<ul>
  <li>
    <a href="http://fabric8.io/guide/mavenPlugin.html">Maven Plugin</a> for working with <a href="http://kubernetes.io/">Kubernetes</a>
  </li>
  <li>
    <a href="http://fabric8.io/guide/testing.html">Integration and System Testing</a> of <a href="http://kubernetes.io/">Kubernetes</a> resources easily inside <a href="http://junit.org/">JUnit</a> with <a href="http://arquillian.org/">Arquillian</a>
  </li>
  <li>
    <a href="http://fabric8.io/guide/javaLibraries.html">Java Libraries</a> and support for <a href="http://fabric8.io/guide/cdi.html">CDI</a> extensions for working with <a href="http://kubernetes.io/">Kubernetes</a>
  </li>
</ul>

### Supported Platforms

Fabric8 works great with [Docker](http://www.docker.com/) and implementations of [Kubernetes](http://kubernetes.io/) such as [Kubernetes itself](http://kubernetes.io/), [OpenShift V3](http://openshift.github.io/), [Project Atomic](http://www.projectatomic.io/) and [Google Container Engine](https://cloud.google.com/container-engine/).

For non-linux platforms which don't yet support native Docker we have [Jube](jube/index.html) which is an open source pure Java implementation of Kubernetes.

Kubernetes is supported on Google and Microsofts clouds, by OpenShift V3 (on premise and public cloud), by Project Atomic and VMware; so it's increasingly becoming the standard API to PaaS and _Container As A Service_ on the open hybrid clouds. [Jube](jube.html) then helps extend Kubernetes to run Java based middleware on any operating system which supports Java 7.

