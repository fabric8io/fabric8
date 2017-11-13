## Spring Boot

Fabric8 provides support for Spring Boot which is meant to make developing spring boot apps for kubernetes easier.

Using Fabric8 it is possible to:

* Inject a fabric8 managed kubernetes client.
* Inject URL to kubernetes service using the @ServiceName.
* Inject a client for a kubernetes service using the @ServiceName and converter beans.
* Inject a client for a kubernetes service using the @ServiceName and @Factory.

fabric8-springboot works for application that live both inside and outside of kubernetes. So it can be used safely in kubernetes and hybrid deployments for easier consumption of kubernetes managed services.

### The @ServiceName annotation
In kubernetes, each service has a host and a port and they are passed to container as environment variable. Containers that live inside Kubernetes
can lookup for services using their environment variables. Application that live outside of kubernetes need to use the kubernetes API in order to perform lookups.

The fabric8 extension provides a unified approach in looking up for service coordinates. It provides the @ServiceName qualifier which can be used to inject the coordinates as a String or as a URL by just referring to the id of the service.

    @Inject
    @ServiceName("my-service")
    private String service.


All available services are registered as io.fabric8.kubernetes.api.model.Service objects. But there are out of the box converter to String and URL.
The URL converter will convert the Service to a URL and the string converter to its string representation.

### The @PortName annotation
In Kubernetes a service may define multiple ports. Fabric8 provides a qualifier which can be used to select a specific port by name.

        @Inject
        @ServiceName("my-service")
        @PortName("my-port")
        private String service.


### Running inside and outside of Kubernetes
Under the covers the code will default to using the **MY_SERVICE_SERVICE_HOST** and **MY_SERVICE_SERVICE_PORT** environment variables exposed by [kubernetes services](services.html) to discover the IP and port to use to connect to the service. Kubernetes sets those environment variables automatically when your pod is run inside Kubernetes.

If your Java code is running outside of Kubernetes then @ServiceName will use the environment variable **KUBERNETES_MASTER** to connect to the kubernetes REST API and then use that to discover where the services are. This lets you run the same Java code in a test, in your IDE and inside kubernetes.

### The @Protocol annotation
Kubernetes uses the notion of Protocol to refer to the transport protocol TCP or UDP. In Java URL it's more useful to use the application protocol.
One could find and replace the transport protocol with the actual application protocol but that's really smelly.

The extension supports the @Protocol annotation which allows the user to specify the application protocol to use.

        @Inject
        @ServiceName("my-ftp-service")
        @Protocol("ftp")
        private String service.

### Using Converters and Factories
Using the service coordinates like the URL is handy. Many times one needs to inject a client/consumer of the service.
Spring is using Converters when it needs to inject a bean and there is a type mismatch. It will look for a converter bean
that can convert from the type of the matching bean in the registry to the type need. If converter is available it will be automatically used.

This module takes things a step further. It provides the @Factory annotation that can automatically create and register a converter for the job.
Here's an example:

       @Factory
       @ServiceName
       public MyClient create(@ServiceName String url) {
           return new MyClient(url);
       }

Note, that using converters should be enough and that this feature is here for compatibility with fabric8-cdi.

#### Injection of Optional Services

If you need to inject optional services or objects, you can use the @Autowired annotation and set the required parameter to false. For example:

    @Autowire(required=false)
    MyClient instance;

