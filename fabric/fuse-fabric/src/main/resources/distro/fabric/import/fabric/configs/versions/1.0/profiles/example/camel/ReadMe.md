## Camel Examples

This folder containers various examples for working [Apache Camel](http://camel.apache.org/) and Fuse:

* [hello](/fabric/profiles/example/camel/hello.profile) the _hello world_ example of using Camel routes defined inside an [XML file inside the profile](/fabric/profiles/example/camel/hello.profile/camel.xml) so you can edit it via the browser and use Fuse Fabric's rolling upgrades to update it and roll forward/backward changes to containers.
* [loanbroker](/fabric/profiles/example/camel/loanbroker.profile) a more complex Loan Broker example using Camel routes to define the loan broker services. This is a particularly good example to use with the [insight capabilities](/fabric/profiles/insight) of Fuse
* [cxf](/fabric/profiles/example/camel/cxf.profile) demonstrates using Camel with [Apache CXF](http://cxf.apache.org/)
* [mq](/fabric/profiles/example/camel/mq.profile) demonstrates using Camel with an [A-MQ message broker profile](/fabric/profiles/mq)