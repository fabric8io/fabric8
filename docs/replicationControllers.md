## Replication Controller

A [Replication Controller](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#labels) is a Kubernetes abstraction which ensures that a specific number of [pods](pods.html) are running at all times. If a pod or host goes down, the replication controller ensures enough pods get recreated elsewhere. 

A _pod template_ is used to define what each pod should look like (e.g. the docker container image to use, it's ports and environment variables etc).

The containers generated can have [labels](labels.html) so that containers can be filtered to create [Services](services.html).
