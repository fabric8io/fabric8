## Jube

For users who cannot use Linux or [go lang](https://golang.org/) compiled binaries or are not ready to go with [Kubernetes](http://kubernetes.io/) / [OpenShift V3](http://openshift.com) or [Docker](http://docker.com), we have a pure Java implementation of Kubernetes called [Jube](http://fabric8.io/jube/getStarted.html).

Jube supports the Kubernetes REST APIs for container orchestration using 100% pure Java; so runs on any platform which has a Java 7 runtime. The motivation for Jube is to primarily support running Java based application servers and services on non-Linux operating systems; we have no plans to try emulate the whole docker ecosystem :). We highly recommend you use Linux and Docker whenever you can; if you can't then Jube can help!

Jube lets you develop all your applications on Kubernetes; even when you need to run Java middleware on operating systems that are not modern linux environments (e.g. AIX, Solaris, Windows) or when Go Lang or Docker are not natively supported.

### Get Started

Here's how to [Get Started with Fabric8 and Jube](getStartedJube.html)