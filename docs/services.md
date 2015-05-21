## Service

A [Service](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#labels) is a kubernetes abstraction to provide a network connection to one or more network services. For more detail see the [background on how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md)

A service uses a label selector to find all the containers running which provide a certain network service on a certain port. The service is then bound to a local port. So to access the service from inside your application or container you just bind to the local network on the port number for the service. 

This means you need to map all services to distinct port numbers. e.g. one for your mysql, one for your ActiveMQ region etc.

### Discovering services from your application

Kubernetes uses 2 environment variables to expose the fixed IP address and port that you can use to access the service.

So for a service named `foo-bar` you can use these 2 environment variables to acces the service...

* `FOO_BAR_SERVICE_HOST` is the host (IP) addresss of the service
* `FOO_BAR_SERVICE_PORT` is the port of the service

e.g. you could access a web site or service via:

    http://${FOO_BAR_SERVICE_HOST}:${FOO_BAR_SERVICE_PORT}/
    
The value of the host and port are fixed once a service is created for the lifetime of a kubernetes environment; so you can just resolve this environment variable on startup and you're good to go!
    
Under the covers Kubernetes will load balance over all the service endpoints for you.
    
Note a [pod](pod.html) can terminate; so its recommended that any network code using a service is capable of retrying requests if a socket fails.
        
### Internal and external services

You can use the Kubernetes [service discovery mechanism](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md) to discover services which are internal and running inside Kubernetes or to discover services running outside of Kubernetes or services on the internet or SaaS providers etc.

For more detail see [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).

### See also

* [how services work](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/services.md)
* [integrating external services](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html)