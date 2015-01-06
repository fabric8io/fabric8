## Testing

Fabric8 supports integration testing of [Apps](apps.html) using Kubernetes via [arquillian-fabric8](https://github.com/fabric8io/fabric8/tree/master/components/arquillian-fabric8) which provides a plugin for [Arquillian](http://arquillian.org/).

[arquillian-fabric8](https://github.com/fabric8io/fabric8/tree/master/components/arquillian-fabric8) uses:

* Kubnernetes to provision and orchestrate the containers
* [Arquillian](http://arquillian.org/) to run the JUnit tests and perform the necessary dependency injection
* [kubernetes-assertions](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-assertions) and [jolokia-assertions](https://github.com/fabric8io/fabric8/tree/master/components/jolokia-assertions) to provide assertions within the JUnit test case.

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
                <version>2.0.18</version>
                <scope>test</scope>
            </dependency>