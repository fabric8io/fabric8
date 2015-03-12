### Working with Environments in Kubernetes

Developers tend to work with multiple environments; production, staging, UAT, regression testing and so forth. Ideally there should be a [Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) pipeline setup to move containers and configurations from dev -> test -> UAT -> staging -> production. In an ideal world we'd reuse as much as possible (container images and [kubernetes app metadata](apps.md) between environments.

In Kubernetes each environment is a **namespace**. Then [pods](pods.html) inside a namespace only see [services](services.html) defined in that namespace. So _prod_ pods only see _prod_ services, _test_ pods only see _test_ services etc.

So [service discovery](services.html) takes care of finding the right database, message brokers, remote caches and other services for the environment (namespace) a pod is provisioned in.


### Reusing JSON/YAML

If you really have to, you can change the kubernetes JSON / YAML on a per environment bases to pass in different environment variables (or even config files mounted into volumes in pods); but the more things are the same between environments, the more likely you really are testing the real stuff in test before production. So if possible; just use service discovery in each environment and leave everything else the same.

You can always use a different, say, database pod in testing containing test data; but ideally you’d reuse the same JSON/YAML and images for your application code.

Incidentally we have an [Arquillian integration testing framework](testing.html) for running complete environments as a test case; it spins up a new namespace, creates however many services/pods/replication controllers, uses Arquillian / JUnit to assert things startup and then performs the actual tests (e.g. querying MBeans and stuff) and then tears everything down at the end.

e.g. here’s [an example integration test](https://github.com/fabric8io/fabric8/blob/master/itests/src/test/java/io/fabric8/itests/BrokerProducerConsumerIT.java#L57) that runs a message broker, producer and consumer and then asserts that the ActiveMQ consumer starts up properly and the ActiveMQ queues enqueue/dequeue correctly. Pretty neat for about a page of code!


So we'd recommend you to try to keep the JSON/YAML the same between environments. One thing that does tend to change between environments is scale. Ideally things would automatically scale; so you use modest sizes in [Replication Controllers](replicationControllers) (e.g. replica size 1); then use per environment auto-scaling to scale things up based on load.

Then your JSON/YAML and docker images can work in all environments without having to change them per environment and folks can have a completely automated Continuous Delivery mechanism setup between however many environments folks need; safe in the knowledge you are testing the same metadata, configuration and images in each environment.

If you do find you need to add some kind of per-environment configuration change, the other option is to put the JSON/YAML in source control and have a branch per environment so you can make environment-specific changes when you absolutely must do (and can then easily diff between environments)