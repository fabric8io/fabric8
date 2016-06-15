## Fabric8 Microservices Platform

**Fabric8** provides a completely integrated open source Microservices Platform which works out of the box on any [Kubernetes](http://kubernetes.io) or [OpenShift](http://www.openshift.org/) environment.

The entire platform is modular and based on _Microservices_ so you can use as much or as little of Microservices Platform as you wish! Its Microservices all the way down!

The available services in **Microservices Platform** are:

* [Developer Console](console.html) is a rich web application which provides a single plane of glass to create, edit, build, deploy and test Microservices
* [Continuous Integration and Continuous Delivery](cdelivery.html) to help teams deliver software in a faster and more reliable way via: 
  * the following open source projects:
    * [Jenkins](https://jenkins.io/) for Building, Continuous Integration and creating Continuous Delivery pipelines
    * [Nexus](http://www.sonatype.org/nexus/) as the artifact repository for caching public artifacts and hosting canary and real release artifacts
    * [Gogs](http://gogs.io/) for on premise git repository hosting and [GitHub](https://github.com/) for public hosting
    * [SonarQube](http://www.sonarqube.org/) provides a platform to maintain code quality
  * [Jenkins Workflow Library](jenkinsWorkflowLibrary.html) to help reuse a [library](https://github.com/fabric8io/jenkins-workflow-library) of reusable [Jenkins Workflow scripts](https://github.com/fabric8io/jenkins-workflow-library) across projects
  * [fabric8.yml file](fabric8YmlFile.html) as a per project configuration file to tie together the various projects, repositories, chat rooms, workflow script and issue tracker
* [ChatOps](chat.html) of all the development and management services via [hubot](https://hubot.github.com/) lets your team embrace devops, have chat notifications of changes to the system and use chat for [approval of release promotion](https://github.com/fabric8io/fabric8-jenkins-workflow-steps#hubotapprove)
* [Chaos Monkey](chaosMonkey.html) to test the resilience of your system by killing [pods](pods.html)!
* [Management](management.html)
    * [Logging](logging.html) provides consolidated logging and visualisation of log statements and events across your environment
    * [Metrics](metrics.html) provides consolidated historical metric collection and visualisation across your environment

### Demo

Here is a [video demonstrating Microservices Platform: Continuous Integration, Continuous Deployment with automatic integration testing, staging and approval for production releases](https://vimeo.com/134408622)

<div class="row">
  <p class="text-center">
      <iframe src="https://player.vimeo.com/video/134408622" width="1000" height="562" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>
  </p>
  <p class="text-center">
    <a href="https://medium.com/fabric8-io/continuous-delivery-with-fabric8-d3c7cad76954">more details</a>
  </p>
</div>

### Installation
    
To install any of the above apps see the [Install Fabric8 on Kubernetes or OpenShift Guide](getStarted/apps.html)    
