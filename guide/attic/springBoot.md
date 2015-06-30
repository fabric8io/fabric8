## Spring Boot

Fabric8 provides an extension which is meant to make developing Spring Boot apps for kubernetes easier.

Using it is possible to:

* Inject a fabric8 managed kubernetes client.
* Inject Service instances using the @Service.

### Running inside and outside of Kubernetes
When the application is run inside Kubernetes, fabric8 will create a spring bean for each service that has defined in the environment.
When the application is run outside Kubernetes, fabric8 will create a spring bean for service returned by the Kubernetes API.

###Using the Kubernetes client

    @Autowired
    private Kubernetes kubernetes;

Or you can use the javax.inject annotation:

    @Inject
    private Kubernetes kubernetes;
    
    
###Injecting Services    

    @Inject
    @Service("my-service")
    Service myService