## Service

A [Service](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#labels) is a kubernetes abstraction to provide a network connection to one or more network services.

A service uses a label selector to find all the containers running which provide a certain network service on a certain port. The service is then bound to a local port. So to access the service from inside your application or container you just bind to the local network on the port number for the service. 

This means you need to map all services to distinct port numbers. e.g. one for your mysql, one for your ActiveMQ region etc.
 