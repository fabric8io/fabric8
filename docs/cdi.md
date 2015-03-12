## CDI

Fabric8 provides a CDI extension which is meant to make developing CDI apps for kubernetes easier.

Using CDI it is possible to:

* Inject a fabric8 managed kubernetes client.
* Inject coordinates to kubernetes service using the @Service.
* Inject a client for a kubernetes service using the @Service.
* Inject configuration provided as Environamnt Variables.

The CDI extension works for application that live both inside and outside of kubernetes. So it can be used safely in kubernetes and hybrid deployments for easier consumption of kubernetes managed services.

### The @Service annotation
In kubernetes, each service has a host and a port and they are passed to container as environment variable. Containers that live inside Kubernetes
can lookup for services using their environment variables. Application that live outside of kubernetes need to use the kubernetes API in order to perform lookups.

The fabric8 extension provides a unified approach in looking up for service coordinates. It provides the @Service qualifier which can be used to inject the coordinates as a String or as a URL by just referring to the id of the service.

    @Inject
    @Service("my-service")
    private String service.

### Running inside and outside of Kubernetes

Under the covers the code will default to using the **MY_SERVICE_SERVICE_HOST** and **MY_SERVICE_SERVICE_PORT** environment variables exposed by [kubernetes services](services.html) to discover the IP and port to use to connect to the service. Kubernetes sets those environment variables automatically when your pod is run inside Kubernetes.

If your Java code is running outside of Kubernetes then @Service will use the environment variable **KUBERNETES_MASTER** to connect to the kubernetes REST API and then use that to discover where the services are. This lets you run the same Java code in a test, in your IDE and inside kubernetes.

### Creating custom objects

In most of the cases the user will create a client so that it can consume the service. This can be easily done by providing a factory.

    @Inject
    @New
    ServiceConverters converters; 
   
    @Produces
    @Service
    public MyClient create(InjectionPoint ip) {
        MyClient result;
        String coords = converters.serviceToString(ip);
        //create the client
        return client;
    }


### Integration with Apache Deltaspike
In kubernetes its quite common to pass around configuration via Environment Variables. Outside of kubernetes java developers often use System properties, property files and what not.
DeltaSpike provides an awesome extension which works great along with the fabric8 extension that allows the user to inject configuration form various sources:

* System Properties
* Environment Variables
* JNDI values
* Property Files

#### The @ConfigProperty annotation
Using the @ConfigProperty annotation the user can inject configuration from the sources mentioned above. The lookup is performed in the exact same order provided above.
It's use is pretty straight-forward:

    @Inject
    @ConfigProperty(name="MY_KEY", defaultValue="MY_VALUE")
    private String key;


#### Putting it all together
In most case, when we need to consume a service we need both the coordinates of the service and static configuration data, like thread pool configuration, maximum number of connections etc.
So it makes sense when we create the "factory" for our client, to also inject configuration. For example:


    @Inject
    @New
    ServiceConverters converters; 
    
    @Inject
    @ConfigProperty(name="CONNECTION_TIMEOUT", defaultValue="10000L")
    private Long timeout; 
   
    @Produces
    @Service
    public MyClient create(InjectionPoint ip) {
        MyClient result;
        String coords = converters.serviceToString(ip);
        //create the client with time out.
        return client;
    }
