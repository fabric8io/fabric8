## Continuous Integration and Continous Delivery

**Microservices Platform** provides a [Continuous Integration and Continous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) (CI and CD) infrastructure built as a set of Kubernetes resources which are easy to [Install](fabric8OnOpenShift.html).

![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Overview

Continuous Integration and Continous Delivery in **Microservices Platform** consists of the following open source components:

* [Jenkins](https://jenkins.io/) for Building, Continuous Integration and creating Continuous Delivery pipelines.
* [Nexus](http://www.sonatype.org/nexus/) as the artifact repository for caching public artifacts and hosting canary and real release artifacts
* [Gogs](http://gogs.io/) for on premise git repository hosting and [GitHub](https://github.com/) for public hosting
* [SonarQube](http://www.sonarqube.org/) provides a platform to maintain code quality

In addition it adds the following optional capabilities:

* [Jenkins Pipeline Library](jenkinsWorkflowLibrary.html) to help reuse a [library](https://github.com/fabric8io/jenkins-workflow-library) of reusable [Jenkins Pipeline scripts](https://github.com/fabric8io/jenkins-workflow-library) across projects
* [fabric8.yml file](fabric8YmlFile.html) as a per project configuration file to tie together the various projects, repositories, chat rooms, workflow script and issue tracker
* [ChatOps](chat.html) via [hubot](https://hubot.github.com/) lets your team embrace devops, have chat notifications of changes to the system and use chat for [approval of release promotion](https://github.com/fabric8io/fabric8-jenkins-workflow-steps#hubotapprove)
* [Chaos Monkey](chaosMonkey.html) to test the resilience of your system by killing [pods](pods.html)!

You can choose to opt in or out of any of the micro services within Microservices Platform; for example use any git repository hosting or Nexus installation. The [Chat](chat.html) integration works with [hubot](https://hubot.github.com/) so that it can work with any back end chat service such as IRC, Slack, HipChat, Campfire etc.
 
By default we try and integrate all the components closely out of the box so **Microservices Platform** is an easy, 1 click install. e.g. by default Jenkins builds will use the local Nexus server for all downloads of maven artifacts and for all canary and full releases.

Since **Microservices Platform** is built on top of Kubernetes we get an easy way to scale (e.g. Jenkins build slaves) together with letting us reuse [Kubernetes services](services.html) for service discovery to wire up the various components (e.g. for Jenkins to discover Nexus). If you opt out of any of the default implementations; you just need to create a [Kubernetes Service pointing to your external installation of the service](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).
 
 
### Automate Jenkins Jobs
 
Teams often have many git repositories with many artifacts and docker container builds. Manually maintaining individual [Jenkins](https://jenkins.io/) build configurations for each git repository can be time consuming and error prone.

So we recommend the use of the [Jenkins Job DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki) to generate the Jenkins build jobs for your projects.

The Jenkins app comes with a template parameter **SEED_GIT_URL** which is the location of the git repository to clone in Jenkins for the Jenkins Job DSL used to generate the Jenkins builds. Any change in that git repository results in the `seed` job being rerun which then regenerates any of its Jenkins jobs. 

The `SEED_GIT_URL` parameter defaults to the value `https://github.com/fabric8io/default-jenkins-dsl.git` for the [default-jenkins-dsl](https://github.com/fabric8io/default-jenkins-dsl) project which provides an example set of scripts to iterate over your projects and create the necessary jobs for them. We hope soon those scripts will automatically setup CI / CD jobs for projects in the hosted [Gogs](http://gogs.io/) repositories (for now it will iterate over a github organisation and generates builds for matching projects).

### Getting started
    
For more information on getting started with Continous Delivery check out the [Fabric8 Getting Started Guide](getStarted/index.html).

### Demo

Here is a [video demonstrating Fabric8 Continuous Delivery Pipelines](https://vimeo.com/170830750)

<div class="row">
  <p class="text-center">
      <iframe src="https://player.vimeo.com/video/170830750" width="1000" height="562" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
  <p class="text-center">
    <a href="https://medium.com/fabric8-io/create-and-explore-continuous-delivery-pipelines-with-fabric8-and-jenkins-on-openshift-661aa82cb45a">more details in a blog post</a>
  </p>
</div>
