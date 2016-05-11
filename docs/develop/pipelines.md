## Pipelines

To go faster  developing microservices its great to automate everything and have a _Continuous Deployment pipeline_ for each microservice. The pipeline then defines how to: 

* build your microservice and create an immutable docker image for it with a unique version label
* generate or enrich the versioned Kubernetes manifest for your microservice 
* run [system and integration tests](../testing.html) on your image to check its working correctly
* deploy your microservice to an environment
* on approval deploy to the production environment

In **fabric8** we use [Jenkins Pipelines](https://jenkins.io/) to implement the pipeline for each microservice. The pipeline is defined in a file called `Jenkinsfile` which is checked into the source repository of each microservice so that its versioned along with the implementation code. 

To get started with your first pipeline, just create a new microservice using the [Developer Console](console.html) which will prompt you to pick a suitable 
pipeline from the [Reusable Pipeline Library](../jenkinsWorkflowLibrary.html)