### Kubernetes Questions

#### What is a microservice in Kubernetes?

Note that the term [microservice](http://martinfowler.com/articles/microservices.html) and [Kubernetes Services](services.html) they are quite different things.

Implementing a microservice on Kubernetes typically comprises of: 

* a [Pod](pods.html) containing the code and configuration of the microservice
* a [Replication Controller](replicationControllers.html) to scale the microservice and keep it running in case of hardware or software failure (including liveness checks to kill dead or hung containers)
* a [Kubernetes Services](services.html) to expose the pods as a network host and port which may well also be [exposed externally](services.html#exposing-services-externally) 

In terms of Kubernetes JSON resources; a micro service is a Replication Controller and Service metadata.

#### How do I do service discovery?

Check the [service discovery docs](services.html#discovering-services-from-your-application).

#### How do I do discover external services?

See [Discovering external services](services.html#discovering-external-services)

#### How do I do expose services externally?

See [Exposing services externally](services.html#exposing-services-externally)

#### How to discover services when running outside of Kubernetes?

See [Discovery when outside of Kubernetes](services.html#discovery-when-outside-of-kubernetes)

#### How do I browse the Swagger docs?

You can browse the Kubernetes REST API using the [Swagger Site](http://kubernetes.io/third_party/swagger-ui/).

To browse the OpenShift Swagger docs for your installation:

* open the swagger JSON URL for your OpenShift master in your browser
    * the URL is [https://vagrant.local:8443/swaggerapi/](https://vagrant.local:8443/swaggerapi/) for the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html)
* if your browser warns you about the certificate continue
    * in chrome: click `Advanced` then `Proceed to vagrant.local (unsafe)` 
* now open the [Swagger Site](http://kubernetes.io/third_party/swagger-ui/) and copy the following URL and paste it into the text field at the top of the page to the right of `swagger`

```
https://vagrant.local:8443/swaggerapi/
```

* hit return on the keyboard or click the `Explore` button and profit!
