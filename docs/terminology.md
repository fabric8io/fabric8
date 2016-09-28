## Terminology

The Fabric8 Microservices Platform uses the following terms:

### Team

A team is a collection of people working together on one or more `microservices`.

A team has a number of `environments` such as Dev, Test, Staging, Production. Each team can decide whatever environments it requires. A team always has a `Development` environment where the development tools run such as Jenkins, Nexus and the [developer console](console.html).

### Environment

An environment is a logical place that a team can run their microservices.

An environment usually maps to a single namespace in a kubernetes cluster; but could map to multiple namespaces in multiple clusters. 

e.g. the `Development` environment for team `Foo` could be namespace `foo-dev`. But their `Production` environment could map to multiple `foo-prod` namespaces in different kubernetes clusters in different data centres/regions.

The `Development` environment is where developer tools run such as Jenkins, Nexus, Forge and the [developer console](console.html). A team always has a development environment. Its up to the team to decide what other environments are required. There is usually something like Test, Staging, Production as well.

Different teams could map environments to the same underlying kubernetes namespace and cluster if they wish. e.g. Staging for team A and team B could share the same underlying namespace if required. Though its usually simpler to adminster and secure if each team have their own namespaces for each environment; then we use [service linking](serviceLinking.html) to share services between teams in environments.

### App

We often use the term `App` to mean an application or microservice. i.e. its something that is built, tested, released and deployed. Usually an App has a build in Jenkins and deploys one or more microservices into environments.
 
When you create or import an App in the Fabric8 Microservices Platform that will have a release pipeline in Jenkins that will build it in the Development environment and then promote it (with optional human approva) through whatever environments are defined by your pipeline.
 
### Setting up teams

One of the core ideas of microservices is about going fast, iterating and continuous improvement. The idea is to make each team operate in parallel with minimal contention.

So we prefer each team to have its own version of the Farbic8 Microservices Platform; so that each team can upgrade at their own pace and have their own Jenkins so that they only see their own builds and pipelines.




