## Jenkins Pipeline Library

Continuous Delivery pipelines can be complex; with things like:

* asynchronous human or integration test approval which can take days to be achieved
* parallel builds
* fan-in and fan-out of builds

Our recommended tool for implementing Continuous Delivery pipelines is Jenkins which version 2.x or later comes with the [Jenkins Pipeline Plugin](https://github.com/jenkinsci/pipeline-plugin) included by default.
                                                                                                                          
The Jenkins Pipeline Plugin lets you to define delivery pipelines using concise [Groovy](http://www.groovy-lang.org/) scripts which deal elegantly with the persistence and asynchrony involved.

You [define your pipeline](https://jenkins.io/doc/pipeline/) in a file called `Jenkinsfile` which we recommend you commit into the root directory of your projects git repository.

### Reusing Pipeline Scripts
 
You can add a Jenkins pipeline file into your project's source code to maintain the delivery pipeline and source code together in the same repository.

However teams often have lots of projects which are very similar in nature and copying a groovy script into each project; or into each jenkins pipeline job configuration page can soon become maintenance burden.

So with Microservices Platform you can configure a reusable _library of pipeline scripts_ such as the [default Fabric8 pipeline library](https://github.com/fabric8io/jenkins-pipeline-library). 
 
Then you can link each project to the reusable pipeline script that most suits the project. The pipeline scripts are configurable with build parameters. If you find that none of the pipeline scripts are quite right for a project, just copy the closest one into the project and customise it.

### Configuring the Pipeline Script for a project

The easiest way to configure the Pipeline for your project is via the [fabric8 developer console](../console.html).

* navigate to the `Team` page then the `App` page
* click on the `Settings` option on the left hand navigation bar
* now click the `Pipeline` tab

If your project has a pipeline you will then have a button to edit it directly in the console.

Otherwise the console will present you with a selection of pipelines that are available to reuse from the [Fabric8 pipeline library](https://github.com/fabric8io/jenkins-pipeline-library). Just pick the one you want and `Next` then fabric8 will add the Jenkinsfile to your project (if you enable the copy operation) and setup the Jenkins build for you.
 
                                                                                                           
