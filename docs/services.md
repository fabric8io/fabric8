## Service

A [Service](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#labels) is a kubernetes abstraction to provide a network connection to one or more [pods](pods.html). For more detail see the [background on how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md).

A service uses a _label selector_ to find all the pods running which provide a certain network service on a port. You can add labels to a [pod](pods.html) which are just a set of key/value pairs. Then a _selector_ is just a set of key/value pairs used to match pods which have those same key/value pairs.

Each service is given its own IP address and port which remains constant for the lifetime of the service. So to access the service from inside your application or container you just bind to the IP address and port number for the service. 

Note make sure the services are created before any of your pods which use them; otherwise the service won't have its IP address defined and you'll have to restart those pods. 

### Discovering services from your application

The simplest way to discover things inside Kubernetes is via DNS which does not require any custom client side code, dependency injection or magic libraries. It also benefits from working with all programming languages and frameworks!

#### Service discovery via DNS

For a service named `foo-bar` you can just hard code the host name `foo-bar` in your application code.

e.g. to access a HTTP URL use `http://foo-bar/` or for HTTPS use  `https://foo-bar/` (assuming the service is using the port 80 or 443 respectively). 

If you use a non standard port number, say, 1234, then append that port number to your URL such as `http://foo-bar:1234/`.

Note that DNS works in kubernetes by resolving to the service named `foo-bar` in the namespace of your pods so you don't have to worry about configuring your application with environment specific configuration or worry about accidentally talking to the production service when in a testing environment!  You can then move your application (its docker image and kubernetes metadata) into any environment and your application works without any changes!

#### Service discovery via environment variables

Kubernetes uses 2 environment variables to expose the fixed IP address and port that you can use to access the service.

So for a service named `foo-bar` you can use these 2 environment variables to access the service:

* `FOO_BAR_SERVICE_HOST` is the host (IP) address of the service
* `FOO_BAR_SERVICE_PORT` is the port of the service

e.g. you could access a web site or service via:

    http://${FOO_BAR_SERVICE_HOST}:${FOO_BAR_SERVICE_PORT}/
    
The value of the host and port are fixed for the lifetime of the service; so you can just resolve the environment variables on startup and you're all set!
    
Under the covers Kubernetes will load balance over all the service endpoints for you.
    
Note a [pod](pod.html) can terminate at any time; so its recommended that any network code should retry requests if a socket fails; then kubernetes will failover to a pod for you.
        
### Discovering external services

You can use the Kubernetes [service discovery mechanism](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md) with DNS or environment variables to discover services which are internal and running inside Kubernetes, running in a different namespace or running outside of Kubernetes such as services on the internet, SaaS providers or provisioned by other means etc.

Basically you just need to create the Service metadata for external services; the difference is you list the actual `Endpoints` rather than letting Kubernetes discover them by using a `pod selector` and watching the running pods.
 
For more detail see [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).

### Exposing services externally
 
Kubernetes service discovery is designed for containers running inside the Kubernetes cluster. The host/ports of services and pods typically are only visible to containers running inside the Kubernetes cluster. 

So for software running outside of a Kubernetes cluster (such as web browsers) to access the services and web applications you need to expose the services externally.

Using the command line you can use the Kubernetes or OpenShift commands:

```
kubectl expose service cheese
oc expose service cheese
```

When using [helm](helm.html) to install or update applications then any service of `type = LoadBalancer` are automatically exposed.

In addition in OpenShift you can [create Route resources directly](http://docs.openshift.org/latest/admin_guide/router.html) which provides a haproxy based external load balancer to accessing services.

If you use Maven you can also use these goals:

* [fabric8:apply](mavenFabric8Apply.html) applies the kubernetes json into a namespace in a kubernetes cluster and by default automatically create any required routes for services
* [fabric8:create-routes](mavenFabric8CreateRoutes.html) generates any missing [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) for the current services in the current namespace 


### Discovery when outside of Kubernetes

When in development its often handy to run your code directly; either inside a docker container or just as a native operating system process; but on your laptop and not inside the kubernetes cluster.

Though even when developing locally we highly recommend to always run your code in a docker image inside Kubernetes via a [vagrant image](getStarted/vagrant.html) as then you can be sure you're really testing your application in docker on the same platform as production together with testing your kubernetes resources too!

To mimick DNS service discovery inside Kubernetes you can just add entries in your `/etc/hosts` file pointing to the host/IP addresses of the services.

For environment variables based discovery, its very easy to setup environment variables so your code can discover services in a remote kubernetes cluster while running on your laptop.

To try this out see:

* [fabric8:create-env](mavenFabric8CreateEnv.html) generates environment variable scripts for Kubernetes [services](services.html) so you can simulate running programs as if they were inside kubernetes

You can then run or debug Java programs in your IDE or via maven while discoverying and reusing services running in a remote Kubernetes cluster.

This is extremely handy in development; though once you've commit your changes to your source control we expect your source code to be turned into immutable docker images and run inside a Kubernetes cluster in the usual way.

### See also

* [how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md)
* [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html)