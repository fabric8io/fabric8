## Fabric ActiveMQ Demo

This example demonstrates using a JMS client acting as a consumer and producer.

The example providers a producer and consumer. The producer sends message to a shared queue,
from where the consumer receives the messages.

This example requires an existing ActiveMQ broker to have been installed first. This can be done from the [MQ](#/fabric/mq/brokers) brokers tab.

### Deploying the example to fabric8

This example can be deployed to fabric8, from the command line using

    mvn fabric8:deploy

And then create a new container using the activemq.demo profile.

Prior to this, a ActiveMQ broker must be running, which you can setup from the [MQ](#/fabric/mq/brokers) brokers tab.

### What happens

When the example runs, you can connect to the container where the ```activemq.demo``` profile is running, and see the logs tab.

You should see the consumer and producer logs how many messages they have sent/received.


