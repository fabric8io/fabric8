## Continuous Delivery

**Fabric8 Continuous Delivery** provides a [Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) infrastructure built as a set of Kubernetes resources which is easy to [Install](fabric8OnOpenShift.html).

![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Overview

**Fabric8 Continuous Delivery** consists of the following parts:

* [Jenkins](https://jenkins-ci.org/) for Building, Continuous Integration and creating Continuous Delivery pipelines.
* [Nexus](http://www.sonatype.org/nexus/) as the artifact repository for caching public artifacts and hosting canary and real release artifacts
* [Gogs](http://gogs.io/) for on premise git repository hosting and [GitHub](https://github.com/) for public hosting
* [SonarQube](http://www.sonarqube.org/) provides a platform to maintain code quality
* [Chat](chat.html) to support social integration between your teams and the infrastructure

You can choose to opt in or out of any of the micro services within Fabric8 Continuous Delivery; for example use any git repository hosting or Nexus installation. The [Chat](chat.html) integration works with [hubot](https://hubot.github.com/) so that it can work with any back end chat service such as IRC, Slack, HipChat, Campfire etc.
 
By default we try and integrate all the components closely out of the box so **Fabric8 Continuous Delivery** is an easy, 1 click install. e.g. by default Jenkins builds will use the local Nexus server for all downloads of maven artifacts and for all canary and full releases.

Since **Fabric8 Continuous Delivery** is built on top of Kubernetes we get an easy way to scale (e.g. Jenkins build slaves) together with letting us reuse [Kubernetes services](services.html) for service discovery to wire up the various components (e.g. for Jenkins to discover Nexus). If you opt out of any of the default implementations; you just need to create a [Kubernetes Service pointing to your external installation of the service](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).
 
 
### Automate Jenkins Jobs
 
Teams often have many git repositories with many artifacts and docker container builds. Manually maintaining individual [Jenkins](https://jenkins-ci.org/) build configurations for each git repository can be time consuming and error prone.

So we recommend the use of the [Jenkins Job DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki) to generate the Jenkins build jobs for your projects.

The Jenkins app comes with a template parameter **SEED_GIT_URL** which is the location of the git repository to clone in Jenkins for the Jenkins Job DSL used to generate the Jenkins builds. Any change in that git repository results in the `seed` job being rerun which then regenerates any of its Jenkins jobs. 

The `SEED_GIT_URL` parameter defaults to the value `https://github.com/fabric8io/default-jenkins-dsl.git` for the [default-jenkins-dsl](https://github.com/fabric8io/default-jenkins-dsl) project which provides an example set of scripts to iterate over your projects and create the necessary jobs for them. We hope soon those scripts will automatically setup CI / CD jobs for projects in the hosted [Gogs](http://gogs.io/) repositories (for now it will iterate over a github organisation and generates builds for matching projects).

### Installation
 
To install this app please see the [Install Fabric8 on OpenShift Guide](fabric8OnOpenShift.html)    
