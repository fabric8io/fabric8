## Elasticity and Resilience

Your microservices should be __highly available__ and resilient to failure. Ideally each microservice should also be _elastic_ so that you can easily scale up or down the number of containers used for each microservice. Some microservices may only require one container; others may require many. 

Fabric8 solves these requirements by using Kubernetes [Replica Sets](../replicationControllers.html) (which used to be called Replication Controllers). 

A _Replia Set_ defines a template for running on or more [pods](../pods.html) which then then be scaled either manually or automatically.

The Replica Set uses a _selector_ to keep watching the available pods matching the selectors labels. If there are not enough pods running it will spin up more; or if there are too many pods running it will terminate the extra pods.

### Manual scaling

To manually scale your Replica Set you just need to specify how many `replicas` you wish by default in your Replica Set YAML file. The default value of 1 should ensure that there is always a pod running. If a pod terminates (or the host running the pod terminates) then Kubernetes will automatically spin up another pod for you.

### Autoscaling

To autoscale you need to annotate your Replica Set with the metadata required, such as CPU limits or custom metrics so that Kubernetes knows when to scale up or down the number of pods.
