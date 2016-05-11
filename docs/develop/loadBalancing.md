## Load Balancing

When you have one or more pods implementing a [service](../services.html) then you need to load balance requests across them.
 
When your microservice is running inside Kubernetes then the [service discovery](serviceDiscovery.html) automatically takes care of load balancing across the available pods for you. 

However if your microservice exposes a web application or API which is intended to be used by users or microservices hosted outside of the Kubenretes cluster, then you need to expose your microservice to an external host endpoint.
 
To expose your microservice you need to create an `Ingress` resource in Kubernetes which will then implement the external load balancer for you. 
 