### General Questions

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?
fabric8 is an integration platform based on Apache ActiveMQ, Apache Camel, Apache CXF, Apache Karaf, Hawtio and others.

It provides automated configuration and deployment management to help make deployments easy, reproducible, and less human-error prone.

Take a look [at this blog post](http://www.christianposta.com/blog/?p=376) to see a more detailed treatment.

#### What does fabric8 do?

fabric8 (pronounced _fabricate_) lets you create and manage fabrics (or clusters) of applications, integrations and middleware.

Try reading the [overview](http://fabric8.io/gitbook/overview.html) to see if that helps give you an idea what fabric8 is.

#### What Java versions is supported?

Currently fabric8 only runs on Java 7. Support for Java 8 is planned and will be available later as part of needed upgrades to Apache Karaf (which is not ready yet).

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.1 release, and is scheduled to be removed in the following release.

The long term replacement is the [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) or if you wish to work with jars rather than bundles then use the [Java Container]((http://fabric8.io/gitbook/javaContainer.html)

