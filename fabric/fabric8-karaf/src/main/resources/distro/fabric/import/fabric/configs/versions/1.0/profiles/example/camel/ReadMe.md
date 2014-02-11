## Camel Examples

This folder containers various examples for working [Apache Camel](http://camel.apache.org/) and Fuse:

* [wiki](/fabric/profiles/example/camel/wiki.profile) an example of using Camel where the routes are stored in an <a fabric-version-link="/camel/canvas/fabric/profiles/example/camel/wiki.profile/camel.xml">XML file insidethe  wiki</a> so you can edit it via the browser and use <a href="/fabric/profiles/docs/fabric/rollingUpgrade.md">rolling upgrades</a> to update it and roll forward/backward changes to containers without having to release any Java artifacts.
* [quickstarts](/fabric/profiles/example/quickstarts) various self contained examples to get you started using [JBoss Fuse](http://www.jboss.org/products/fuse)
  * [cbr](/fabric/profiles/example/quickstarts/cbr.profile) demonstrates how to use a Content Based Router using [Apache Camel](http://camel.apache.org/)
  * [cbr.wiki](/fabric/profiles/example/quickstarts/cbr.wiki.profile) is the same Content Based Router demo but where the [Camel route](http://camel.apache.org/) is stored inside the wiki so it can be easily changed via the Management Console
  * [eip](/fabric/profiles/example/quickstarts/eip.profile) demonstrates how to use Enterprise Integration Patterns using [Apache Camel](http://camel.apache.org/)
  * [errors](/fabric/profiles/example/quickstarts/errors.profile) demonstrates how to perform Error Handling in [Apache Camel](http://camel.apache.org/)
* [loanbroker](/fabric/profiles/example/camel/loanbroker) a more complex Loan Broker example using Camel routes to define the loan broker services. This is a particularly good example to use with the [insight capabilities](/fabric/profiles/insight) of Fuse
* [cxf](/fabric/profiles/example/camel/cxf.profile) demonstrates using Camel with [Apache CXF](http://cxf.apache.org/)
* [mq](/fabric/profiles/example/camel/mq.profile) demonstrates using Camel with an [A-MQ message broker profile](/fabric/profiles/mq)
* [twitter](/fabric/profiles/example/camel/twitter.profile) demonstrates using Camel to route tweeets from twitter serached using keyword(s).
 
