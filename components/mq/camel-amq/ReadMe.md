## Camel AMQ Component

The Camel **amq:** component uses the [Kubernetes Service](http://fabric8.io/guide/services.html) discovery mechanism to discover and connect to the ActiveMQ brokers. So you can just use the endpoint directly; no configuration is required.

e.g. just use the camel endpoint **"amq:Cheese"** to access the Cheese queue in ActiveMQ; no configuration required!

The Camel **amq:** is used in the example **Fabric8 MQ Producer** and **Fabric8 MQ Consumer** apps in the **Library** in the [console](http://fabric8.io/guide/console.html)

For more information check out the [Fabric8 MQ documentation](http://fabric8.io/guide/fabric8MQ.html)

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8.mq</groupId>
                <artifactId>camel-amq</artifactId>
                <version>2.2.96</version>
            </dependency>

