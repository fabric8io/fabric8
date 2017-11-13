## Pipelines

To go faster  developing Microservices its great to automate everything and have a _Continuous Deployment pipeline_ for each Microservice. The pipeline then defines how to:

* build your Microservice and create an immutable docker image for it with a unique version label
* generate or enrich the versioned Kubernetes manifest for your Microservice
* run [system and integration tests](../testing.html) on your image to check it's working correctly
* deploy your Microservice to an environment
* on approval deploy to the production environment

In **fabric8** we use [Jenkins Pipelines](https://jenkins.io/) to implement the pipeline for each Microservice. The pipeline is defined in a file called `Jenkinsfile` which is checked into the source repository of each Microservice so that it's versioned along with the implementation code.

To get started with your first pipeline, just create a new Microservice using the [Developer Console](console.html) which will prompt you to pick a suitable
pipeline from the [Reusable Pipeline Library](../jenkinsWorkflowLibrary.html)
