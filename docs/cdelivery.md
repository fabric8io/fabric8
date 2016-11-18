## Continuous Integration and Continuous Delivery

**fabric8** provides [Continuous Integration and Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) (CI and CD) infrastructure built as a set of Kubernetes resources which are easy to [Install](getStarted/index.html).

![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Overview

Continuous Integration and Continuous Delivery in **fabric8** consists of the following open source components:

* [Jenkins](https://jenkins.io/) for Building, Continuous Integration and creating Continuous Delivery pipelines.
* [Nexus](http://www.sonatype.org/nexus/) as the artifact repository for caching public artifacts and hosting canary and real release artifacts
* [Gogs](http://gogs.io/) for on premise git repository hosting and [GitHub](https://github.com/) for public hosting

In addition it adds the following optional capabilities:

* [Jenkins Pipeline Library](https://github.com/fabric8io/fabric8-pipeline-library) to help reuse Jenkins Pipeline functions and steps across projects. 

To use the functions and steps in this library just add the following to the top of your `Jenkinsfile`:

```groovy
@Library('github.com/fabric8io/fabric8-pipeline-library@master')
```

* [Jenkinsfile Library](https://github.com/fabric8io/fabric8-jenkinsfile-library) is a library of reusable `Jenkinsfile` files you can copy into your project. This library is also used by the [developer console](console.html) to provide the choice of pipelines when creating a project (via the underlying JBoss Forge wizard)

* [fabric8.yml file](fabric8YmlFile.html) as a per project configuration file to tie together the various projects, repositories, chat rooms, workflow script and issue tracker
* [ChatOps](chat.html) via [hubot](https://hubot.github.com/) lets your team embrace devops, have chat notifications of changes to the system and use chat for [approval of release promotion](https://github.com/fabric8io/fabric8-jenkins-workflow-steps#hubotapprove)
* [Chaos Monkey](chaosMonkey.html) to test the resilience of your system by killing [pods](pods.html)!
* [SonarQube](http://www.sonarqube.org/) provides a platform to maintain code quality

You can choose to opt in or out of any of the micro services within fabric8; for example use any git repository hosting or Nexus installation. The [Chat](chat.html) integration works with [hubot](https://hubot.github.com/) so that it can work with any back end chat service such as IRC, Slack, HipChat, Campfire etc.
 
By default we try and integrate all the components closely out of the box so **fabric8** is an easy, 1 click install. e.g. by default Jenkins builds will use the local Nexus server for all downloads of maven artifacts and for all releases.

Since **fabric8** is built on top of Kubernetes we get an easy way to scale (e.g. Jenkins build slaves) together with letting us reuse [Kubernetes services](services.html) for service discovery to wire up the various components (e.g. for Jenkins to discover Nexus). If you opt out of any of the default implementations; you just need to create a [Kubernetes Service pointing to your external installation of the service](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).
 
 
### Discovering Jenkins Jobs

With the advent of Jenkins Pipelines (included by default from version Jenkins 2.0) its considered good practice to define your pipline in a file called `Jenkinsfile` and to copy that into the root folder of your git repository.

#### GitHub Organisation plugin

The [github organisation plugin](https://wiki.jenkins-ci.org/display/JENKINS/GitHub+Organization+Folder+Plugin) can be configured to automatically iterate through all repositories for a given organisation and then for each repository it will find all branches with a `Jenkinsfile` and automatically setup a build job for the pipeline. Then all the build jobs appear in the [developer console](console.html). 


#### Multibranch plugin

The [pipeline multibranch plugin](https://wiki.jenkins-ci.org/display/JENKINS/Pipeline+Multibranch+Plugin) automatically creates a jenkins job for every branch in a repository that has a `Jenkinsfile`. Then all the build jobs appear in the [developer console](console.html).  


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
