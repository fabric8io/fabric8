## Camel Examples

This folder containers various examples for working [Apache Camel](http://camel.apache.org/) and Fuse:

* [hello](/fabric/profiles/example/camel/hello.profile) the _hello world_ example of using Camel routes defined inside an <a fabric-version-link="/camel/canvas/fabric/profiles/example/camel/hello.profile/camel.xml
">XML file inside the profile</a> so you can edit it via the browser and use Fuse Fabric's rolling upgrades to update it and roll forward/backward changes to containers.
* [quickstarts](/fabric/profiles/example/quickstarts) various self contained examples to get you started using [JBoss Fuse](http://www.jboss.org/products/fuse)
  * [cbr](/fabric/profiles/example/quickstarts/cbr.profile) demonstrates how to use a Content Based Router using [Apache Camel](http://camel.apache.org/)
  * [eip](/fabric/profiles/example/quickstarts/eip.profile) demonstrates how to use Enterprise Integration Patterns using [Apache Camel](http://camel.apache.org/)
  * [errors](/fabric/profiles/example/quickstarts/errors.profile) demonstrates how to perform Error Handling in [Apache Camel](http://camel.apache.org/)
* [loanbroker](/fabric/profiles/example/camel/loanbroker.profile) a more complex Loan Broker example using Camel routes to define the loan broker services. This is a particularly good example to use with the [insight capabilities](/fabric/profiles/insight) of Fuse
* [cxf](/fabric/profiles/example/camel/cxf.profile) demonstrates using Camel with [Apache CXF](http://cxf.apache.org/)
* [mq](/fabric/profiles/example/camel/mq.profile) demonstrates using Camel with an [A-MQ message broker profile](/fabric/profiles/mq)