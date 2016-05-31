## Configuration

As developers we usually try to make the software we create easy to configure so that it can be used in any environment.

However building microservices on the cloud changes our approach to configuration since you no longer need to configure the following differently for each environment:

* file locations: since docker images have the same file system layout whatever the environment
* service locations: instead use [service discovery](serviceDiscovery.html)
* secrets like usernames and passwords: instead use [kubernetes secrets](http://kubernetes.io/docs/user-guide/secrets/)

So it turns out that many microservices no longer need any configuration values which change for different environments thanks to the above!

This helps reduce possible errors; it means most configuration for your microservice can be included inside the docker image and tested in every environment in your [CI / CD Pipeline](http://fabric8.io/guide/cdelivery.html)

However for times when you really need to configure things differently on a per environment/deployment basis then here are the main approaches:

### Include configuration inside the docker image

The simplest thing to do with configuration is just include it inside your docker image. e.g. if you are building a Spring Boot microservice then just include the `application.properties` or `application.yml` file inside your `src/main/resources` folder so that it gets included into your jar and the docker image. This then means that all your configuration gets tested as part of your [CI / CD Pipeline](http://fabric8.io/guide/cdelivery.html)

This assumes the configuration will be the same in each environment due to the above (using [service discovery](serviceDiscovery.html) and [kubernetes secrets](http://kubernetes.io/docs/user-guide/secrets/)).

For any environment specific values then you could specify environment variables in your kubernetes resource. e.g. your [Kubernetes Deployments](http://kubernetes.io/docs/user-guide/deployments/) can include `env` values in the pod template's container section.

Though its good practice maximise the amount of immutable arfifacts you have (e.g. docker images and kubernetes resources) and to minimise the amount of artifacts you need to modify for each environment. So for per-environment configuration we recommend one of the following approaches:

### ConfigMap

Kubernetes supports a resource called [ConfigMap](http://kubernetes.io/docs/user-guide/configmap/) which is used to store **environment specific** configuration values - all other configuration values should be inside the docker image as described above.

You can then expose the `ConfigMap` resources either as volume mounts or as environment variables.

If you are using Spring in your Java based microservices you may want to check out the [ConfigMap based PropertySource](https://github.com/fabric8io/spring-cloud-kubernetes#configmap-propertysource) that makes it really easy to load environment specific configurations along with any other configuration included in your docker image.


### Git

One of the downsides of using `ConfigMap` as described above is that there's no history or change tracking; there's just the latest version of the ConfigMap. For complex configuration its very useful to have a changelog so you can see who changed what configuration values when so that when things start to go wrong you can easily revert changes or see the history.

So you can store your environment specific configuration in a git repository (maybe using a different branch or repo for each environment) then you can mount the git repository as a volume in your microservice docker container via a [`gitRepo` volume](http://kubernetes.io/docs/user-guide/volumes/#gitrepo) using a specific git revision.

You can then use the [gitcontroller](https://github.com/fabric8io/gitcontroller) microservice to watch the [Kubernetes Deployments](http://kubernetes.io/docs/user-guide/deployments/) with one or more [`gitRepo` volumes](http://kubernetes.io/docs/user-guide/volumes/#gitrepo) and it then watches for changes in the associated git repositories and branches.

When there are changes in a configuration git repository the `gitcontroller` will perform a rolling upgrade of the [Kubernetes Deployments](http://kubernetes.io/docs/user-guide/deployments/) to use the new configuration git  revision; or rollback. The rolling upgrade policy (e.g. speed and number of concurrent pods which update and so forth) is all specified by your [rolling update configuration in the Deployment specification](http://kubernetes.io/docs/user-guide/deployments/#rolling-update-deployment).

Here is an [example of how to add a `gitRepo` volume to your application](https://github.com/jstrachan/springboot-config-demo/blob/master/src/main/fabric8/deployment.yml#L5-L14); in this case a spring boot application to load the [`application.properties`](https://github.com/jstrachan/sample-springboot-config/blob/master/application.properties) file from a git repository.

You can either run `gitcontroller` as a microservice in your namespace or you can use the `gitcontroller` binary at any time or as part of your [CI / CD Pipeline](http://fabric8.io/guide/cdelivery.html) process.

### Choosing the right approach

We recommend you try to keep the environment specific configuration down to a minimum as that increases the amount of resuable immuable artifacts (docker images and kubernetes resources) that can be used without modification in any environment and promotes confidence and testing of those artifacts across your [CI / CD Pipeline](http://fabric8.io/guide/cdelivery.html).

So if you can avoid any configuration that is environment specific (thanks to [service discovery](serviceDiscovery.html) and [kubernetes secrets](http://kubernetes.io/docs/user-guide/secrets/)) then just including configuration inside your docker image makes the most sense.

When you absolutely must have some environment specific configuration then our recommendation is:

* if you want history and to be able to see who changes what and when so you can easily audit or revert changes then use `git`. Otherwise use `ConfigMap`



