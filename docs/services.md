## Service

A [Service](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#labels) is a kubernetes abstraction to provide a network connection to one or more [pods](pods.html). For more detail see the [background on how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md).

A service uses a label selector (a set of key/value pairs) to find all the pods running which provide a certain network service on a port. 

Each service is given its own IP address and port which remains constant for the lifetime of the service. So to access the service from inside your application or container you just bind to the IP address and port number for the service. 

Note make sure the services are created before any of your pods which use them; otherwise the service won't have its IP address defined and you'll have to restart those pods. 

### Discovering services from your application

Kubernetes uses 2 environment variables to expose the fixed IP address and port that you can use to access the service.

So for a service named `foo-bar` you can use these 2 environment variables to access the service:

* `FOO_BAR_SERVICE_HOST` is the host (IP) address of the service
* `FOO_BAR_SERVICE_PORT` is the port of the service

e.g. you could access a web site or service via:

    http://${FOO_BAR_SERVICE_HOST}:${FOO_BAR_SERVICE_PORT}/
    
The value of the host and port are fixed once a service is created for the lifetime of a kubernetes environment; so you can just resolve the environment variables on startup and you're all set!
    
Under the covers Kubernetes will load balance over all the service endpoints for you.
    
Note a [pod](pod.html) can terminate at any time; so its recommended that any network code ushould retry requests if a socket fails; then kubernetes will failover to a pod for you.
        
### Internal and external services

You can use the Kubernetes [service discovery mechanism](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md) to discover services which are internal and running inside Kubernetes or to discover services running outside of Kubernetes or services on the internet or SaaS providers etc.

For more detail see [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).

### See also

* [how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md)
* [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html)