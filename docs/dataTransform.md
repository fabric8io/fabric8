## Data Transform

There are various tools supported in [Apache Camel](http://camel.apache.org/) for performing data transformation and implementing the [Message Translator pattern](http://camel.apache.org/message-translator.html). For example there's always Java code via the [Bean Integration](http://camel.apache.org/bean-integration.html) or using [XQuery](http://camel.apache.org/xquery-endpoint.html) if you need to transform XML into XML.

Our recommended general purpose transformation engine for taking any kind of Java object and converting it into any other Java object is [Dozer](http://dozer.sourceforge.net/documentation/about.html) as

* it has great [camel integration](https://camel.apache.org/dozer-type-conversion.html) which works great with [Camel's inbuilt type conversion repository](http://camel.apache.org/type-converter.html)
* its fast and doesn't pretend things are all XML or SQL underneath like some transformation frameworks do; instead it uses the most efficient JVM level representations of objects (e.g. beans)
* rather like Camel it has a Java and XML DSL for defining the transformations; plus is has a nice [hawtio](http://hawt.io/) [plugin](http://hawt.io/plugins/dozer/) for viewing and editing the mapping.

The idea then is to use the different [Data Formats](http://camel.apache.org/data-format.html) in Camel to unmarshal data into some Java object structure; or to marshal it from a Java object structure to some data format. Then we have the best flexibility; all the Data Formats, Java objects as the in memory structure and Dozer for the transformation engine.

### Getting started with Dozer and fabric8

The easiest way to get started is:

* [download and run fabric8](getStarted.html)
* create a new container for the [example-dozer](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/example/dozer.profile) profile
* you should now be able to view the [example-dozer profile page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/example/dozer.profile) then click on the [dozerMapping.xml](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/dozer/mappings/fabric/profiles/example/dozer.profile/dozerMapping.xml) to open the visual editor
