## Integration and System Testing

Fabric8 supports integration testing of [Apps](apps.html) using Kubernetes via [fabric8-arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) which provides an extension for [Arquillian](http://arquillian.org/).

Using this extension you can easily:

* Apply the kubernetes configuration of your application, including its dependencies.
* Wait until pods and services are ready to be used.
* Make created services, replication controllers and kubernetes client available inside your test case (via @ArquillianResource).

[fabric8-arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) uses:

* Kubnernetes to provision and orchestrate the containers inside a new unique namespace; so that each test case is isolated from other environments and test cases
* [Arquillian](http://arquillian.org/) to run the JUnit tests and perform the necessary dependency injection
* [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) and [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) to provide assertions within the JUnit test case.

### Session, Lifecycle &amp; Labels

The kubernetes configuration is applied once per test suite. This means that the environment is getting created once per test suite and then multiple test cases are run against that environment.
To encapsulate everything that has been applied to kubernetes as part of the current test suite, the notion of "session" is used.

In order to distinguish which pods, services and replication controllers have been created as part of the testing session, anything that is created using the arquillian extension, will be created inside a unique namespace per session.
This namespace is also going to be used to cleanup everything after the end of the suite or upon termination of the process.

The Session is also made available to the test cases as an arquillian resource _(see below)_.

### Keeping the namespace around

If a test fails you may want to look inside the Kubernetes namespace took at the failed pods. By default the namespace gets garbage collected immediately.

To delay this you can use these environment variables

* FABRIC8_NAMESPACE_CONFIRM_DESTROY set this to 'true' to force the user to type 'Q' in the command line terminal to terminate the namespace
* FABRIC8_NAMESPACE_DESTROY_TIMEOUT set this to the number of seconds to keep the namespace around for before destroying it

### Arquillian Resources available to test cases

A typical integration test, would need to apply the kubernetes configuration, wait for everything to start up and then assert that that expected pods and services are available and of course test the actual services.

To do that, the test needs to have access to information like:

* Pods
* Services
* Replication Controllers
* Session Namespace

Each of the items above is made available to the test as an arquillian resource. 


To obtain a reference to the KubernetesClient:

     @ArquillianResource
     io.fabric8.kubernetes.api.KubernetesClient client;

#### Services

To obtain the list of all services created in the current session:

    @ArquillianResource
    io.fabric8.kubernetes.api.model.ServiceList sessionServices;


To obtain a reference to a particular service created in the current session:


    @Id("my-service-id")
    @ArquillianResource
    io.fabric8.kubernetes.api.model.Service myService;

#### Replication Controllers


To obtain the list of all replication controllers created in the current session:

    @ArquillianResource
    io.fabric8.kubernetes.api.model.ReplicationControllerList sessionControllers;

To obtain a refernce to a particular replication controller created in the current session:


    @Id("my-controller-id")
    @ArquillianResource
    io.fabric8.kubernetes.api.model.ReplicationController myController;
        

To obtain the list of all pods created in the current session:
      
    @ArquillianResource
    io.fabric8.kubernetes.api.model.PodList sessionPods;
        
To obtain the Session:

     @ArquillianResource
     io.fabric8.arquillian.kubernetes.Session mySession;
        
### Configuration Options

Any configuration option can be provided as an environment variable, system property or arquillian property. 
For example:
        
        <arquillian xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns="http://jboss.org/schema/arquillian"
                    xsi:schemaLocation="http://jboss.org/schema/arquillian
                    http://jboss.org/schema/arquillian/arquillian_1_0.xsd">
                    
            <extension qualifier="kubernetes">
                <property name="masterUrl">true</property>
            </extension>
        </arquillian>

The only required configuration option is url to the kubernetes master. Since in most cases this will be specified as KUBERNETES_MASTER env variable, there is no need
to specify it again. 
This means that the arquillian.xml configuration file is completely optional.
        
Supported options:
        
* masterUrl: The url to the kubernetes master.
* configFileName: If a url hasn't been explicitly specified, the configFileName can be used for discovery of the configuration in the classpath.
* configUrl: The url to the kubernetes configuration to be tested.
* connectToServices: Whether or not an attempt is made to connect to a service port; failing the test if it can't be connected. This is disabled by default since its likely a service PortalIP / port cannot be opened by the JUnit test case (and may require authentication)
* dependencies: A space separated list of directories, files or urls to kubernetes configurations that are required to be applied before the current one.
* waitForServiceConnection: Wait until a network connection to all applied services is possible.
* serviceConnectionTimeout: The connection timeout for each attempt to "connect to the service".
* waitForServices: Explicitly specify which services to wait. If this option is ommitted or empty all services will be waited.
* timeout: The total amount of time for to wait for pods and service to be ready.
* pollInterval: The interval between polling the status of pods and services.


### Maven Integration

If dependencies have not been explicitly specified, the extension will find all test scoped dependencies and search inside those artifacts for kubernetes.json resources.
Then it will treat those as dependencies and apply them along with the configuration.

One thing to be careful of is that adding test dependencies on kubernetes.json will add transitive dependencies to your test project; so you may want to add exclusions as [this example shows](https://github.com/fabric8io/fabric8/blob/master/itests/pom.xml#L57).

### Example

There are lots of examples in the [quickstarts](quickstarts/index.md).

Here is an [example Arquillian Fabric8 integration test](https://github.com/fabric8io/fabric8/blob/master/itests/src/test/java/io/fabric8/itests/BrokerProducerConsumerIT.java#L57) that tests that an AMQ broker, producer and consumer startup and properly produce and consume messages correctly.

In particular [here is the code that does JMX assertions via jolokia on the containers](https://github.com/fabric8io/fabric8/blob/master/itests/src/test/java/io/fabric8/itests/BrokerProducerConsumerIT.java#L74) that are created by the integration test.

### Requirements

When running an **fabric8-arquillian** integration test then the environment variable **KUBERNETES_MASTER** needs to be specified to point to the kubernetes environment in which to create the containers and services.

Also you may want to set the **KUBERNETES_TRUST_CERTIFICATES** variable to allow connection to kubernetes without a client certificatE:

    export KUBERNETES_TRUST_CERTIFICATES=true
    export KUBERNETES_MASTER=http://localhost:8443

Note that this can be any kubernetes environment (a kubernetes installation, OpenShift, RHEL Atomic or GKE). Also note that different integration tests can be running at the same time on the same kubernetes environment.

If you find that you are getting exceptions and output of the form:

    Waiting for: SomeException....
    Waiting for: SomeException....

and if the exception isn't clear from the message, you could define this environment variable to get an additional full stack trace (which is very noisy but can be handy):

    export FABRIC8_VERBOSE_ASSERT=true

### Assertion libraries

The following libraries are provided to help you create concise assertions using [assertj](http://joel-costigliola.github.io/assertj/) based DSL:

 * [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) provides a set of [assertj](http://joel-costigliola.github.io/assertj/) assertions of the form **assertThat(kubernetesResource)** for working with the [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api)
 * [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) makes it easy to perform assertions on remote JVMs via JMX using  [Jolokia](http://jolokia.org/) over HTTP/JSON
 * [kubernetes-jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) makes it easy to work with the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) and Java containers running in [Pods](http://fabric8.io/guide/pods.html) inside Kubernetes which expose the Jolokia port

### Add it to your Maven pom.xml

To be able to use this library add this to your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-arquillian</artifactId>
                <version>2.2.96</version>
                <scope>test</scope>
            </dependency>