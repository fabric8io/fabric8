Pomegranate Overview
====================

So the aim of the Pomegranate module is to provide a class loader model that maps easily to the traditional dependency
model used by most popular Java build tools these days (Maven, Ant+Ivy, Gradle, SBT (Simple Build Tool) et al).

Given a list of maven style dependencies (and transitive dependencies) we create what feels like a simple flat class
loader. So it should feel and act just like running tests in Maven / Ant+Ivy / Gradle / SBT).

However rather than just using a flat class loader (after all if you just wanted a flat class loader in an application
server you can just use a WAR right now) we want to promote sharing of code across deployments (WARs or bundles).
Plus by avoiding the need to embed jars inside WARs, artifacts should be smaller & rebuild/redeploy times should increase.


Overview
--------

So we take the pom.xml from WEB-INF/pom.xml for a WAR or META-INF/pom.xml for a jar/bundle and we then walk the
transitive dependencies.

We ensure that

* when sharing there's only ever 1 version of any artifact (denoted by a single groupId and artifactId pair) added to the
ClassLoader for a pom.xml - so it feels like a simple flat class loader like using one of the common maven goals...

  * mvn test
  * mvn jetty:run
  * mvn war

* we try and share class loaders across deployments. For example if you deploy a number of applications using the
same versions of Apache Camel (and all its transitive dependencies) then the class loader is shared.

* we can easily enumerate the exact versions we're using (a bit like running mvn dependency:tree) in case users are
confused over what files are being used on the class loader. Indeed it should be easy to generate the CLASSPATH
variable that can be used on the command line to mimick the same class loader mechanism outside of OSGi.

* we also make it easy for folks by reporting potential conflicts (e.g. maybe warning when we override
transitive dependencies etc)


