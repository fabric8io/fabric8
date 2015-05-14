## iPaaS

**Fabric8 iPaaS** provides an _Integration Platform As A Service_ consisting of:

* [Console](console.html) provides a nice web console based on [hawtio](http://hawt.io/) for working with your integration services
* [API Registry](apiRegistry.html) provides a global view of all of your RESTful and web service APIs that is displayed in the [Console](console.html) allowing you to inspect and invoke all the endpoints
* [MQ](fabric8MQ.html) implements _Messaging As A Service_ with [Apache ActiveMQ](http://activemq.apache.org/) on Kubernetes.
* [MQ AutoScaler](fabric8MQAutoScaler.html) monitors and scales the [Apache ActiveMQ](http://activemq.apache.org/) brokers running on Kubernetes

Then to help you and your team provision and manage your integration services:

* [Management](management.html) consolidated [Logging](logging.html)and  [Metrics](metrics.html) to visualise and diagnose your integration solutions
* [Continuous Delivery](cdelivery.html) provides a development, building, releasing and provisioning pipeline
* [Chat](chat.html) provides a [hubot](https://hubot.github.com/) [app](apps.html) and a notification engine to post [build completion](builds.html) events to a chat room (which defaults to one room per kubernetes namespace).

