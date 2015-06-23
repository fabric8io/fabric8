## fabric8:delete-pods

The maven `fabric8:delete-pods` goal queries all the [Pods](pods.html) in the current Kubernetes namespace to find all the pods using the current maven projects docker image and deletes them.

This will then cause the [replication controllers](replicationControllers.html) in Kubernetes to recreate the pods again.

This is a useful way in development to quickly upgrade to a new docker image.

## Quickly try docker images locally

When running with `DOCKER_HOST` and `KUBERNETES_MASTER` pointing at your local [fabric8 vagrant image](http://fabric8.io/guide/getStartedVagrant.html) if you are inside a project you can run:

```
mvn clean install docker:build fabric8:delete-pods
```

Then the following happens:

* the maven project will be rebuilt
* the docker image is built inside the docker daemon in the [fabric8 vagrant image](http://fabric8.io/guide/getStartedVagrant.html) 
* all current pods using this image are then deleted, causing the [replication controllers](replicationControllers.html) to recreate the pods again using the newly created image.

i.e. you should now have all the pods running in your vagrant image updated to the new docker image you just built.