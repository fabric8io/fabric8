## Kubernetes Assertions

This library provides a bunch of helpful [assertj](http://joel-costigliola.github.io/assertj/) assertions for working with the [kubernetes-api](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api).

[Here is an example of the assertThat(kubernetesResource)](https://github.com/fabric8io/fabric8/blob/master/components/kubernetes-assertions/src/test/java/io/fabric8/kubernetes/assertions/Example.java#L38) helper code that is available if you add the **kubernetes-assertions** dependency.

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>kubernetes-assertions</artifactId>
                <version>2.2.96</version>
                <scope>test</scope>
            </dependency>
