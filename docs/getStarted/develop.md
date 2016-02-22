## Develop locally

Pushing and pulling docker images to and from docker registries can be quite slow, even when everything is running on your laptop. So its useful to avoid that step when working locally as it speeds up your `edit -> compile -> run` cycle time and helps you develop faster.

So if you are running Kubernetes locally on your laptop such as via the [Fabric8 vagrant image](vagrant.html) its a good idea to use the same docker daemon that is running inside vagrant on your host operating system (OS X / Windows) when using the docker command and building images.

### Setup your machine

Set this environment variable if you use the [Fabric8 vagrant image](vagrant.html):

    export DOCKER_HOST=tcp://vagrant.f8:2375

And don't define any of the other docker env vars like `DOCKER_CERT_PATH` or `DOCKER_TLS_VERIFY`.

Now when in a shell on your host (OS X / Windows) you can type `docker ps` and the output is the same as if you type the same command inside the `vagrant ssh` shell inside the vagrant VM.

This then means that your host (OS X / Windows) will use the same docker daemon as the Kubernetes running inside the fabric8 vagrant image. Because of this you don't need to use a docker registry; a docker daemon is kinda like a local registry anyway - provided you don't try to push or pull the image! 

**NOTE**  you need to make sure that your kubernetes JSON uses the imagePullPolicy of `PullIfAbsent` (via the maven property `fabric8.imagePullPolicy`); as any attempt to pull an image just built locally that isn't pushed to a docker registry won't work; since the image isn't public yet. In fabric8 2.2.96 the default in `mvn fabric8:json` is to now omit this value which means it uses the kubernetes default which is `PullIfAbsent`.


### Build and apply
 
So in a Maven project type the following:

    mvn install docker:build fabric8:json fabric8:apply

This will then build the docker image - in the fabric8 vagrant image - and then generate and apply the kubernetes JSON. This should then run your application on kubernetes. Kubernetes should not need to try pull the image from anywhere since its local in the docker daemon and it should startup fine.

If it doesn't try to start up - or you see it keep retrying from the openshift logs - then there could be an issue with your docker image or code. So its a good idea to try just run it yourself on the console.

    docker run -itP imageName

Ideally passing in the environment variables your image needs to discover other services. There's a handy command, [mvn fabric8:create-env](../mavenFabric8CreateEnv.html) to figure out the env vars for you so that you can run docker images outside of kubernetes as if they are inside (in terms of service discovery and environment variables defined in the kubernetes json).


### Apply new images faster

Rather than doing the whole regeneration of the JSON and reapplying (which is fairly fast but can take a couple of seconds) and doing a docker push/pull (which can take many seconds) you can just do:

    mvn install docker:build fabric8:delete-pods

For more background see a [description of the mvn fabric8:delete-pods command](../mavenFabric8DeletePods.html).

What this does is builds the docker image in the docker daemon and deletes all the pods in kubernetes which use the same image as your current maven project; this immediately causes the kubernetes [Replication Controller](../replicationControllers.html) to recreate any of the pods you deleted. 

Basically this causes an immediate rolling upgrade to the new image! Its a pretty nice, quick way of working.