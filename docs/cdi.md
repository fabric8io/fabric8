## CDI

Fabric8 provides a CDI extension which is meant to make developing CDI apps for kubernetes easier.

Using CDI it is possible to:

* Inject a fabric8 managed kubernetes client.
* Inject URL to kubernetes service using the @Service.
* Inject a client for a kubernetes service using the @Service and @Factory.
* Inject configuration provided as Environamnt Variables.
* Create scopes of configuration using the @Configuration.

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
   
    @Produces
    @Service
    public MyClient create(InjectionPoint ip) {
        Service service = ip.getAnnotated().getAnnotation(Service.class);
        String url = Services.toServiceUrl(service.getId(), service.getProtocol());
        return new MyClient(url);
    }

### Using Factories
The example above is something that works but it does require boilerplate (and other limitation which are discussed below).
To simplify the code Fabric8 provides the @Factory annotation.
   
       @Factory
       @Service
       public MyClient create(@Service String url) {
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
    @Service
    public MyClient create(@Service url) {
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
                
                
#### Combining @Factory, @Service and @Configuration annotations
To further reduce the amount of boilerplate one could use all of the annotations to create generic factories that accepts as parameters service urls and configuration.
              
    public class MyFactory {
        @Factory
        @Service
        MyClient create(@Service String url, @Configuration MyConfig cfg) {
            return MyClient(url, cfg);  
        }
    }
    
The you can directly inject the client:
    
        @Inject
        @Service("SERVICE_1")
        private MyClient cl1;
        
        @Inject
        @Service("SERVICE_2")
        private MyClient cl2;        
        
In this approach the benefit is double as both the configuration and the service url are not needed to be specified in the factory but just to the place where the client is consumed. That makes the factory reusable and reduces the amount of code needed.