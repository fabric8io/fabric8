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

#### How many namespaces should I use?

Namespaces are a great way in kubernetes to group related [pods](pods.html) and [services](services.html) together to get [easy service discovery](services.html#discovering-services-from-your-application) without requiring environment-specific configuration or service linking. They also provide role based authentication (RBAC). 

The more related apps ([pods](pods.html) and [services](services.html)) are in the same namespace, the easier it is [for service discovery](services.html#discovering-services-from-your-application) and the less configuration or service linking you need to do. The flip side is, the more namespaces you have, the easier it is to get finer grained RBAC policies on who can view or change apps in the namespace.

So our recommendation is use a namespace for each environment (e.g. Dev, Test, Staging, Production) for each team. You can then define who has what roles in each team; then you can define the environments for each team. 

You may find that namespaces can be shared across teams (e.g. maybe Staging contains the apps from multiple teams) so use your best judgement. There’s nothing stopping you just having, say, 1 namespace for all your production apps; but given companies often split ‘all of production’ into separate teams who look after different parts; its probably a case where you refactor “production” into separate production teams and they each have their own namespace per environment (they may want a Staging and Production environment for example).

In a pure microservice world, you may have 1 namespace per environment per microservice; though that might be a bit granular for every environment (you may wish to colocate multiple microservices in the same namespace). Though even a single microservice might be a collection of pods and services - whether its cassandra, kafka, riak, elasticsearch or whatever. Really a microservice just represents a ‘chunk’ of a monolith thats built, released & managed by a separate independent team. 

If you do go the direction of one microservice per namespace; you’ll have all the Kubernetes Services you need for your app in your namespace; but they will be remote (implemented typically outside your namespace) so you’ll use [external services or service linking](services.html#exposing-services-externally) to point to the implementations you need (using _Endpoints_ rather than using the usual _pod selectors_ in [Kubernetes services](service.html)


#### How do I browse the Swagger docs?

You can browse the Kubernetes REST API using the [Swagger Site](http://kubernetes.io/third_party/swagger-ui/).

To browse the OpenShift Swagger docs for your installation:

* open the swagger JSON URL for your OpenShift master in your browser
    * the URL is [https://vagrant.f8:8443/swaggerapi/](https://vagrant.f8:8443/swaggerapi/) for the [fabric8 Vagrant image](getStarted/vagrant.html)
* if your browser warns you about the certificate continue
    * in chrome: click `Advanced` then `Proceed to vagrant.f8 (unsafe)` 
* now open the [Swagger Site](http://kubernetes.io/third_party/swagger-ui/) and copy the following URL and paste it into the text field at the top of the page to the right of `swagger`

```
https://vagrant.f8:8443/swaggerapi/
```

* hit return on the keyboard or click the `Explore` button and profit!
