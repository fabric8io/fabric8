## Integration

**Fabric8 Integration** provides an _Integration Platform As A Service_ consisting of:

* [Developer Console](console.html) provides a nice web console based on [hawtio](http://hawt.io/) for working with your integration services
* [API Registry](apiRegistry.html) provides a global view of all of your RESTful and web service APIs that is displayed in the [Console](console.html) allowing you to inspect and invoke all the endpoints
* [Messaging](fabric8MQ.html) implements elastic and scalable _Messaging As A Service_ with [Apache ActiveMQ Artemis](http://activemq.apache.org/artemis/) on Kubernetes.

Then to help you and your team provision and manage your integration services:

* [Management](management.html) consolidated [Logging](logging.html) and  [Metrics](metrics.html) to visualise and diagnose your integration solutions
* [Continuous Delivery](cdelivery.html) provides a development, building, releasing and provisioning pipeline
* [ChatOps](chat.html) provides a [hubot](https://hubot.github.com/) [app](apps.html) and a notification engine to post [build completion](builds.html) events to a chat room (which defaults to one room per kubernetes namespace).
* [API Management](apiManagement.md) allows you to add governance and metrics to your integration services

### Installation

To install this app please see the [Install Fabric8 on OpenShift Guide](getStarted/apps.html)
