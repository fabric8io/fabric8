### Running a quickstart on Jube

We are going to run a quickstart in Jube using the [mvn fabric8:run goal](mavenPlugin.html#running). Type the following:

    git clone https://github.com/fabric8io/quickstarts.git
    cd quickstarts
    mvn install
    cd quickstarts/java/camel-spring
    mvn clean install fabric8:json fabric8:run

If the above fails it could be you have not setup your [environment variables](getStartedJube.html#setting-environment-variables) so that the maven plugin can communicate with kubernetes REST API.

Once you have run the **mvn fabric8:run** command you should see it succeed. If you hit any issues check out the [FAQ](http://fabric8.io/v2/FAQ.html), [get in touch](http://fabric8.io/community/index.html) or [raise an issue](https://github.com/fabric8io/fabric8/issues)

Now you should be able to see any created kubernetes resources in the tabs: [Pods](http://localhost:8585/hawtio/kubernetes/pods), [Replication Controllers](http://localhost:8585/hawtio/kubernetes/replicationControllers) or [Services](http://localhost:8585/hawtio/kubernetes/services) in the web console. Note that the first thing to be created are the [replication controllers](replicationControllers.html) which then try to start the pods.

#### What just happened?

Jube supports the Kubernetes REST API for orchestrating containers which uses JSON to define [pods](pods.html), [replication controllers](replicationControllers.html) or [services](services.html).

In the above example we used the maven tooling to generate the Kuberenetes JSON for a quickstart (to define [replication controller](replicationControllers.html) which will then create a [pod for the container](pods.html) and keep it running) and then POST the JSON to Jube via the Kubernetes REST API to create the [replication controller](replicationControllers.html).

The [mvn fabric8:run goal](mavenPlugin.html#running) then [generated the kubernetes JSON](mavenPlugin.html#generating-the-json) and POSTed it to the Jube server at the URL defined by the **KUBERNETES_MASTER** [environment variable](getStartedJube.html#setting-environment-variables).

The build will [generated](mavenPlugin.html#generating-the-json) a **target/classes/kubernetes.json** file as part of the build (the **fabric8:json** goal does that), then the **fabric8:run** goal will use the Kubernetes REST API to create the necessary [pods](pods.html), [replication controllers](replicationControllers.html) or [services](services.html).

### Troubleshooting

If maven builds fail it may be you have not setup the [environment variables](getStartedJube.html#setting-environment-variables) correctly; see above.

Otherwise check out the [Jube Troubleshooting Guide](http://fabric8.io/jube/troubleshooting.html) for details of how to check the logs and web console.

#### Using another maven project

Note if you are trying this on your own maven project you may wish to add the [jube:build goal to your projects package goal](http://fabric8.io/jube/mavenPlugin.html#adding-the-plugin-to-your-project) or you can run the **jube:build** goal via

    mvn clean install jube:build fabric8:json fabric8:run

#### If a pod fails to start

Sometimes you may see a pod not start; it may then try again and create more pods. If you hover over the status icon on the [Pods tab](http://localhost:8585/hawtio/kubernetes/pods) you should see a tooltip explanation for the failure (or click on the pod and look at the detail view).

Usually this means that Jube could not find the [image zip](http://fabric8.io/jube/imageZips.html) in a maven repository for the docker container names referenced in your pod JSON. You may want to check that you properly built the associated image zip and it's available in a maven repository at the maven coordinates that the exception reports it used.

#### Scaling your replication controllers

When you are running some [replication controllers](replicationControllers.html) you can use the [Replication Controllers tab](http://localhost:8585/hawtio/kubernetes/replicationControllers) and update the replica count (then hit **Save**) and in a few moments you should see Jube create or destroy pods so that the current running system matches your required number of replicas.

Another approach is to specify the replica count in your pom.xml via the **fabric8.replicas** [property](mavenPlugin.html#properties-for-configuring-the-generation). For example to run 2 pods (containers) for a project try::

    mvn fabric8:run -Dfabric8.replicas=2

#### Stopping pods

The easiest way to stop pods is to set the number of replicas to 0 on the [Replication Controllers tab](http://localhost:8585/hawtio/kubernetes/replicationControllers). Then delete the replication controller if you wish.

