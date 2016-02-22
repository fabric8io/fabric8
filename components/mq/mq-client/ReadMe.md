## MQ Client

This library provides the **io.fabric8.mq.core.MQConnectionFactory** class which is a JMS ConnectionFactory which connects to ActiveMQ using the [Kubernetes Service]http://fabric8.io/guide/(services.html) discovery mechanism described.

Just create the MQConnectionFactory and it will use the environment variables of the current process to discover the ActiveMQ brokers.

For more information check out the [Fabric8 MQ documentation](http://fabric8.io/guide/fabric8MQ.html)

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8.mq</groupId>
                <artifactId>mq-client</artifactId>
                <version>2.2.96</version>
            </dependency>

