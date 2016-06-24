## CDI

Fabric8 provides a CDI extension which is meant to make developing CDI apps for kubernetes easier.

Using CDI it is possible to:

* Inject a fabric8 managed kubernetes client.
* Inject URL to kubernetes service using the @ServiceName and @PortName annotations.
* Inject a client for a kubernetes service using the @ServiceName and @Factory.
* Inject configuration provided as Environamnt Variables.
* Create scopes of configuration using the @Configuration.

The CDI extension works for application that live both inside and outside of kubernetes. So it can be used safely in kubernetes and hybrid deployments for easier consumption of kubernetes managed services.

### The @ServiceName annotation
In kubernetes, each service has a host and a port and they are passed to container as environment variable. Containers that live inside Kubernetes
can lookup for services using their environment variables. Application that live outside of kubernetes need to use the kubernetes API in order to perform lookups.

The fabric8 extension provides a unified approach in looking up for service coordinates. It provides the @ServiceName qualifier which can be used to inject the coordinates as a String or as a URL by just referring to the id of the service.

    @Inject
    @ServiceName("my-service")
    private String service.

### The @Protocol annotation
Kubernetes uses the notion of Protocol to refer to the transport protocol TCP or UDP. In Java URL its more useful to use the application protocol.
One could find and replace the transport protocol with the actual application protocol but that its really smelly.

The CDI extension supports the @Protocol annotation which allows the user to specify the application protocol to use.

        @Inject
        @ServiceName("my-ftp-service")
        @Protocol("ftp")
        private String service.
        
### The @PortName annotation
In Kubernetes a service may define multiple ports. Fabric8 provides a qualifier which can be used to select a specific port by name.

        @Inject
        @ServiceName("my-service")
        @PortName("my-port")
        private String service.

If for a multiport service no @PortName qualifier is specified, the first port on the list will be used.

### The @Path annotation
In the same spirit with the @Protocol annotation this extension also supports the @Path annotation, for the cases that we need the injected value to be decorated with a custom path.

                @Inject
                @ServiceName("mysqldb")
                @Protocol("jdbc:mysql")
                @Path("db1")
                private String urlForDb1.
                
                @Inject
                @ServiceName("mysqldb")
                @Protocol("jdbc:mysql")
                @Path("db2")
                private String urlForDb2.                        

### The @Endpoint annotation
There are cases, where we don't want to access the Service IP but we want to access and endpoint to the service.
A very good example are headless service, which doesn't have a Service IP. You can instruct the framework to use endpoints instead of the Service with the @Endpoint annotation.

        @Inject
        @ServiceName("headless-service")
        @Endpoint
        private String service.

If more than one endpoints are available for the service, the first found will be the one injected. But you can access them all by using a List or a Set.

        @Inject
        @ServiceName("headless-service")
        @Endpoint
        private List<String> services.
        
In case of Set or List injection the @Endpoint annotation can also be assumed.        

### Running inside and outside of Kubernetes

Under the covers the code will default to using the **MY_SERVICE_SERVICE_HOST** and **MY_SERVICE_SERVICE_PORT** environment variables exposed by [kubernetes services](services.html) to discover the IP and port to use to connect to the service. Kubernetes sets those environment variables automatically when your pod is run inside Kubernetes.

If your Java code is running outside of Kubernetes then @ServiceName will use the environment variable **KUBERNETES_MASTER** to connect to the kubernetes REST API and then use that to discover where the services are. This lets you run the same Java code in a test, in your IDE and inside kubernetes.

### Integration with OpenShift routes
In any case if a Route is available that match the Service we need to inject, the route host will be used instead.
Currently route will be used only if @Protocol or @PortName haven't been explicitly specified and only if OpenShift is available.

### Creating custom objects
In most of the cases the user will create a client so that it can consume the service. This can be easily done using the injection point and a manual kubernetes service lookup.
   
    @Produces
    @ServiceName
    public MyClient create(InjectionPoint ip) {
        Service service = ip.getAnnotated().getAnnotation(Service.class);
        String url = Services.toServiceUrl(service.getId(), service.getProtocol());
        return new MyClient(url);
    }
    
### Using Factories
The example above is something that works but it does require boilerplate.
To simplify the code Fabric8 provides the @Factory annotation.
   
       @Factory
       @ServiceName
       public MyClient create(@ServiceName String url) {
           return new MyClient(url);
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


#### Putting it all together
In most case, when we need to consume a service we need both the coordinates of the service and static configuration data, like thread pool configuration, maximum number of connections etc.
So it makes sense when we create the "factory" for our client, to also inject configuration. For example:


    @Inject
    @ConfigProperty(name="CONNECTION_TIMEOUT", defaultValue="10000L")
    private Long timeout; 
   
    @Produces
    @ServiceName
    public MyClient create(@ServiceName url) {
        return new MyClient(url, timeout);
    }

But what happens if we need  to create multiple instances of MyClient each configured differently?

#### The @Configuration annotation
The configuration annotation allows you to group a set of configuration properties together as part of a bean and instantiate it with different configuration values, depending the context.

For example let's assume that **MyClient** which has been used in the previous examples, requires a timeout and a pool size for its configuration. And we need to configure multiple instances of MyClient.
The configuration of MyClient can be encapsulated like this:

    public class MyConfig {
        private final Integer poolSize;
        private final Long timeout;
    }
        
The obvious way would be to create 2 classes for MyConfig each using a different set of annotations. The worst part is that then we would also need to create 2 instances of @Produces each consuming a different set of configuration. Which becomes really painful even for a simple example like this.

The alternative approach is to use the @Configuration annotation and a simple convention: "For all configuration sets, keys are named the same and use a descriminator prefix". This would allow use to use a single MyConfig class and only specifiy the **descriminator** where we need to.
     
     public class MyConfig {
         @Inject
         @ConfigProperty(name="POOL_SIZE", defaultValue="10")         
         private final Integer poolSize;
         
         @Inject
         @ConfigProperty(name="CONNECTION_TIMEOUT", defaultValue="10000L")
         private final Long timeout;
     }


The discriminator only needs to be specified at the point where configuration is consumed.

        @Inject
        @Configuration("SERVICE_1")
        private MyConfig cfg1;
        
        @Inject
        @Configuration("SERVICE_2")
        private MyConfig cfg2;
                
                
#### Combining @Factory, @ServiceName and @Configuration annotations
To further reduce the amount of boilerplate one could use all of the annotations to create generic factories that accepts as parameters service urls and configuration.
              
    public class MyFactory {
        @Factory
        @ServiceName
        MyClient create(@ServiceName String url, @Configuration MyConfig cfg) {
            return MyClient(url, cfg);  
        }
    }
    
The you can directly inject the client:
    
        @Inject
        @ServiceName("SERVICE_1")
        private MyClient cl1;
        
        @Inject
        @ServiceName("SERVICE_2")
        private MyClient cl2;        
        
In this approach the benefit is double as both the configuration and the service url are not needed to be specified in the factory but just to the place where the client is consumed. That makes the factory reusable and reduces the amount of code needed.

Factories can also make use of the @Protocol and @PortName annotations to set default values for protocol and port name.

    public class MyDataSourceFactory {
        @Factory
        @ServiceName
        DataSource create(@ServiceName @Protocol("jdbc:mysql") @PortName("mysqld-port") String url, @Configuration MyConfig cfg) {
            DataSource ds = null;
            ...
            return ds;  
        }
    }
    
If @Protocol or @PortName are present on the actual injection point to, they will take precedence over what's found here.    

#### Injection of Optional Services

A common approach for treating with Optional services in CDI is to use Instance injection. For example:

    @Inject
    Instance<MyClient> instance;

    ...

    MyClient client = instance.get();

The Fabric8 CDI extension supports instance injection for all kinds of injection including @Factory.
