## Service Linking

The idea behind _service linking_ is to allow [Teams](terminology.html) to operate in their own [Environments](terminology.html) with their own access control, security and quotas; yet be able to easily share and reuse microservices between teams.
 
For example Team A may not be able to change Team B's microservices and vice versa; Team B can export whatever microservices they wish to be consumed by Team A.

In Kubernetes if a team wishes to consume services from other teams, it creates a [Kubernetes Service](service.html) for each required microservice - but without selectors. Then Endpoints can be created to implement the Service by pointing to services implemented by other teams.

There is work going on with the [Service Catalog]() to improve the usability and human workflow of the service linking.