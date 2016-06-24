## Jenkins Workflow Library

Continuous Delivery pipelines can be complex; with things like:

* asynchronous human or integration test approval which can take days to be achieved
* parallel builds
* fan-in and fan-out of builds

So to allow flexible Continuous Delivery Pipelines to be defined in a simple script (or DSL) there is an excellent Jenkins plugin called [Jenkins Workflow Plugin](https://github.com/jenkinsci/workflow-plugin).
 
The Jenkins Workflow Plugin lets you to define delivery pipelines using concise [Groovy](http://www.groovy-lang.org/) scripts which deal elegantly with the persistence and asynchrony involved.
 
### Reusing Workflow Scripts
 
You can add a Jenkins workflow script into your project's source code to maintain the delivery pipeline and source code together in the same repository.

However teams often have lots of projects which are very similar in nature and copying a groovy script into each project; or into each jenkins workflow job configuration page can soon become maintenance burden.

So with Microservices Platform you can configure a reusable _library of workflow scripts_ such as the [default Fabric8 workflow library](https://github.com/fabric8io/jenkins-workflow-library). 
 
Then you can link each project to the reusable pipeline script that most suits the project. The pipeline scripts are configurable with build parameters. If you find that none of the workflow scripts are quite right for a project, just copy the closest one into the project and customise it.


### Configuring the Workflow Script for a project

To configure the reuseable workflow script for a project you can use the [fabric8.yml file](fabric8YmlFile.html).

You can edit the `fabric8.yml` file via:

* the `project settings` button in the [console](console.html)
* the `fabric8.yml` file directly using a text editor in source control
* using the [Microservices Platform Forge commands from the CLI or in your IDE](forge.html)


