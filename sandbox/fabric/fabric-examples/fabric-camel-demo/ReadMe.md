## Fabric Camel and ActiveMQ Demo

This example demonstrates using a Camel route to route messages over ActiveMQ.

This example requires an existing ActiveMQ broker to have been installed first. This can be done from the [MQ](#/fabric/mq/brokers) brokers tab.

### Deploying the example to fabric8

This example can be deployed to fabric8, from the command line using

    mvn fabric8:deploy

And then create a new container using the camel.activemq.demo profile.

Prior to this, a ActiveMQ broker must be running, which you can setup from the [MQ](#/fabric/mq/brokers) brokers tab.

### What happens

When the example runs, you can connect to the container where the ```activemq.camel.demo``` profile is running, and see the logs tab.

You should see how many messages Camel has processed.


