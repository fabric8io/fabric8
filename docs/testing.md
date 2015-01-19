## Testing

Fabric8 supports integration testing of [Apps](apps.html) using Kubernetes via [arquillian-fabric8](https://github.com/fabric8io/fabric8/tree/master/components/arquillian-fabric8) which provides an extension for [Arquillian](http://arquillian.org/).

Using this extension you can easily:

* Apply the kubernetes configuration of your application, including its dependencies.
* Wait until pods and services are ready to be used.
* Make created services, replication controllers and kubernetes client available inside your test case (via @ArquillianResource).

[arquillian-fabric8](https://github.com/fabric8io/fabric8/tree/master/components/arquillian-fabric8) uses:

* Kubnernetes to provision and orchestrate the containers
* [Arquillian](http://arquillian.org/) to run the JUnit tests and perform the necessary dependency injection
* [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) and [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) to provide assertions within the JUnit test case.

### Session, Lifecycle & Labels

The kubernetes configuration is applied once per test suite. This means that the environment is getting created once per test suite and then multiple test cases are run against that enviornment.
To encapsulate everything that has been applied to kubernetes as part of the current test suite, the notion of "session" is used.

In order to distinguish which pods, services and replication controllers have been created as part of the testing session, anything that is created using the arquillian extension, will be added a label with key arquillian and a unique session id.
This label is also going to be used to cleanup everything after the end of the suite or upon termination of the process.

The Session is also made available to the test cases as an arquillian resource _(see below)_.


### Arquillian Resources available to test cases

A typical integration test, would need to apply the kubernetes configuration, wait for everything to start up and then assert that that expected pods and services are available and of course test the actual services.

To do that, the test needs to have access to information like:

* Pods
* Services
* Replication Controllers
* Session Id

Each of the items above is made available to the test as an arquillian resource. 


To obtain a reference to the KubernetesClient:

               @ArquillianResource
               io.fabric8.kubernetes.api.KubernetesClient client;

#### Services

To obtain the list of all services created in the current session:

        @ArquillianResource
        io.fabric8.kubernetes.api.model.ServiceList sessionServices;


To obtain a refernce to a particular service created in the current session:


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
* configUrl: The url to the kubernetes configuration to be tested.
* configFileName: If a url hasn't been explicitly specified, the configFileName can be used for discovery of the configuration in the classpath.
* dependencies: A space separated list of urls to kubernetes configurations that are required to be applied before the current one.
* waitForServiceConnection: Wait until a network connection to all applied services is possible.
* serviceConnectionTimeout: The connection timeout for each attempt to "connect to the service".
* waitForServices: Explicitly specify which services to wait. If this option is ommitted or empty all services will be waited.
* timeout: The total amount of time for to wait for pods and service to be ready.
* pollInterval: The interval between polling the status of pods and services.


### Maven Integration

If dependencies have not been explicitly specified, the extension will find all test scoped dependencies and search inside those artifacts for kubernetes.json resources.
Then it will treat those as dependencies and apply them along with the configuration.


### Example

There are lots of examples in the [quickstarts](http://fabric8.io/v2/quickstarts.html).

Here is an [example Arquillian Fabric8 integration test](https://github.com/fabric8io/quickstarts/blob/master/apps/jadvisor/src/test/java/io/fabric8/apps/jadvisor/JadvisorKubernetesTest.java#L42)

### Assertion libraries

The following libraries are provided to help you create concise assertions using [assertj](http://joel-costigliola.github.io/assertj/) based DSL:

 * [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) provides a set of [assertj](http://joel-costigliola.github.io/assertj/) assertions of the form **assertThat(kubernetesResource)** for working with the [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api)
 * [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) makes it easy to perform assertions on remote JVMs via JMX using  [Jolokia](http://jolokia.org/) over HTTP/JSON
 * [kubernetes-jolokia](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-jolokia) makes it easy to work with the [Jolokia Client API](http://jolokia.org/reference/html/clients.html#client-java) and Java containers running in [Pods](http://fabric8.io/v2/pods.html) inside Kubernetes which expose the Jolokia port

### Add it to your Maven pom.xml

To be able to use this library add this to your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>arquillian-fabric8</artifactId>
                <version>2.0.19</version>
                <scope>test</scope>
            </dependency>