## Fabric8 Arquillian Extension

Fabric8 provides an arquillian extension, which helps you write and run integration tests for your Kubernetes/Openshift application.

### Overview

This extension, wll create and manage a temporary namespace for your tests, apply all Kubernetes resources required to create your environment and once everything is ready it will run your tests.

This extension, will not mutate neither your containers *(by deploying, reconfiguring etc)* nor your Kubernetes resources, and takes a black box approach to testing.

### Features
- Hybrid *(in or out of Kubernetes/Openshift)*
- Advanced namespace management
- Dependency management *(for maven based projects)*
- Management of Secrets via [Secret Annotations](secretAnnotations.html)
- Enrichers for:
    - Kubernetes/Openshift client
    - Pods
    - Replication Controllers
    - Services
- Integration with Fabric8 Modules:
    - [Fabric8 Maven Plugin](mavenPlugin.html)
    - [Microservices Platform](fabric8DevOps.html)
    - [Fabric8 CDI](cdi.html)
- "Bring your own client" support

### Configuring the extension

The plugin can be configured using the traditional arquillian.xml, via system properties or evnironment variables (in that particular order).
Which means that for every supported configuration parameter, the arquillian.xml will be looked up first, if it doesn't contain an entry, the system properties will be used.
If no result has been found so far the environment variables will be used.

**Note:** When checking for environment variables, property names will get capitalized, and symbols like "." will be converted to "_".
For example **fabric8.foo.bar.baz** will be converted to **FABRIC8_FOO_BAR_BAZ**.


| Option                              | Type         | Env | Description                                                                  |
|-------------------------------------|--------------|------------------------------------------------------------------------------------|
| kubernetes.master                   | URL          | Any | The URL to the Kubernetes master                                             |
| kubernetes.domain                   | String       | OSE | Domain to use for creating routes for services                               |
| fabric8.environment                 | String       | Any | The testing environment to use (used for selecting namespace)                |
| namespace.use.existing              | String       | Any | Don't generate a namespace use the specified one instead                     |
| namespace.lazy.enabled              | Bool (true)  | Any | Should the specified namespace be created if not exists, or throw exception? |
| namespace.cleanup.enabled           | Bool (true)  | Any | Flag to destroy the namespace after the end of the test suite                |
| namespace.cleanup.confirm.enabled   | Bool (false) | Any | Flag to ask for confirmation to delete the namespace                         |
| namespace.cleanup.timeout           | Long         | Any | Time to wait when cleaning up the namespace                                  |
| env.init.enabled                    | Bool (true)  | Any | Flag to initialize the environment (apply kubernetes resources)              |
| env.config.url                      | URL          | Any | URL to the Kubernetes JSON (defaults to classpath resource kubernetes.json)  |
| env.config.resource.name            | String       | Any | Option to select a different classpath resource (ohter than kubernetes.json) |
| env.dependencies                    | List         | Any | Whitespace separated list of URLs to more dependency kubernetes.json         |
| wait.timeout                        | Long (5mins) | Any | The total ammount of time to wait until the env is ready                     |
| wait.poll.interval                  | Long (5secs) | Any | The poll interval to use for checking if the environment is ready            |
| wait.for.service.list               | Long (5secs) | Any | Explicitly specify a list of service to wait upon                            |
| wait.for.service.connection.enabled | Bool (false) | Any | Flag to specify if we should wait for an actual connection to the service    |
| wait.for.service.connection.timeout | Long (10secs)| Any | The amount of time we should wait for each socket connection.                |
| ansi.logger.enabled                 | Bool (true)  | Any | Flag to enable colorful output                                               |
| kubernetes.client.creator.class.name| Bool (true)  | Any | Fully qualified class name of a kubernetes client creator class (byon)       |

### Namespaces

The default behavior of the extension is to create a unique namespace per test suite. The namespace is created Before the suite is started and destroyed in the end.
For debugging purposes, you can set the **namespace.cleanup.enabled** to false and keep the namespace around.

In other cases you may find it useful to manually create and manage the environment rather than having **fabric8-arquillian** do that for you.
In this case you can use the **namespace.use.existing** option to select an existing namespace. This option goes hand in hand with **env.init.enabled** which can be
used to prevent the extension from modifying the environment.

Last but not least a namespace can be indirectly specified by selecting a **fabric8.environment**, e.g. *Testing*.
In this case the extension will look for a [fabric8.yml](fabric8YmlFile.html) and will try to find the namespace to use for the specified environment.

### Creating the environment
After creating or selecting an existing namespace, the next step is the environment preparation. This is the stage where all the required Kubernetes configuration will be applied.
Out of the box, the extension will use the classpath and try to find a resource named **kubernetes.json**. The name of the resource can be changed using the **env.config.resource.name**.
Of course it is also possible to specify an external resource by URL using the **env.config.url**.

Either way, it is possible that the kubernetes configuration used, depends on other configurations. It is also possible that your environment configuration is split in multiple files.
To cover cases like this the **env.dependencies** is provided which accepts a space separated list of URLs.

**Note:** Out of the box mvn urls are supported, so you can use values like: **mvn:my.groupId/artifactId/1.0.0/json/kubernetes**

**Also:** If your project is using maven and dependencies like the above are expressed in the pom, the will be used *automatically*.


### Readiness and waiting
Creating an environment does not guarantee its readiness. For example a Docker image may be required to get pulled by a remote repository and this make take even several minutes.
Running a test against a Pod which is not Running state is pretty much pointless, so we need to wait until everything is ready.

This extension will wait up to **wait.timeout** until everything is up and running. Everything? It will wait for all Pods and Service *(that were created during the test suite initialization)* to become ready.
It will poll them every **wait.poll.interval** milliseconds. For services there is also the option to perform a simple "connection test"  by setting the flag **wait.for.service.connection.enabled** to true.
In this case it will not just wait for the service to ready, but also to be usable/connectable.

### Immutable infrastructure and integration testing

As mentioned in the overview, this extension will not try to deploy your tests, inside an application container.
It doesn't need nor want to know what runs inside your docker containers, nor will try to mess with it.
It doesn't even need to run inside Kubernetes (it can just run in your laptop and talk to the kubernetes master).

So what exactly is your test case going to test?

The test cases are meant to consume and test the provided services and assert that the environment is in the expected state.

The test case may obtain everything it needs, by accessing the Kubernetes resources that are provided by the plugin as @ArquillianResources (see resource providers below).

### Resource Providers

The resource providers available, can be used to inject to your test cases the following resources:

- A kubernetes client as an instance of KubernetesClient
- Session object that contains information (e.g. the namespace) or the uuid of the test session.
- Pods *(by id or as a list of all pods created during the session)*
- Replication Controllers *(by id or as a list of all replication controllers created during the session)*
- Services *(by id or as a list of all services created during the session)*
- Jolokia Clients

 Here's a small example:

    @RunWith(Arquillian.class)
    public class ExampleTest {

     @ArquillianResource
     KubernetesClient client;

     @ArquillianResource
     Session session;

      @Test
      public void testAtLeastOnePod() throws Exception {
       assertThat(client).pods().runningStatus().filterNamespace(session.getNamespace()).hasSize(1);
      }
    }

The test code above, demonstrates how you can inject an use inside your test the *KubernetesClient* and the *Session* object. It also demonstrates the use of **kubernetes-assertions** which is a nice little library based on [assert4j](http://assertj.org) for performing assertions on top of the Kubernetes model.

The next example is intended to how you can inject a resource by id.

    @RunWith(Arquillian.class)
    public class ResourceByIdTest {

     @ArquillianResouce
     @ServiceName("my-serivce")
     Service service;

     @ArquillianResouce
     @PodName("my-pod")
     Pod pod;

     @ArquillianResouce
     @ReplicationControllerName("my-contoller")
     ReplicationController controller;

      @Test
      public void testStuff() throws Exception {
       //Do stuff...
      }
    }

The next example is intended to how you can inject a resource list.

    @RunWith(Arquillian.class)
    public class ResourceListExample {

     @ArquillianResouce
     ServiceList services;

     @ArquillianResouce
     PodList pods;

     @ArquillianResouce
     ReplicationControllers controllers;

      @Test
      public void testStuff() throws Exception {
       //Do stuff...
      }
    }

Using the service object one can create a URL to the service, connect to it and perform any kind of operations.
For java based application in particular one can directly obtain a jolokia client for inspecting directly the target jvm. For example:

    @RunWith(Arquillian.class)
    public class JolokiaClientTest {

     @ArquillianResouce
     @ServiceName("target-serivce")
     J4pClient jolokia;

      @Test
      public void testWithJolokia() throws Exception {
        assertThat(jolokia).doubleAttribute("java.lang:type=OperatingSystem", "SystemCpuLoad").isGreaterThanOrEqualTo(0.0);
      }
    }
