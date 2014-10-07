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

fabric8 runs on Java 7 and 8. 

#### Is Windows supported

Windows is only partially supported. 

For example the [process manager](http://fabric8.io/gitbook/processManager.html#process-management) does not yet support windows; as it relies on running scripts which currently only supports linux systems. Likewise creaitng containers using [SSH](http://fabric8.io/gitbook/sshContainers.html) is not supported by Windows. Neither is [docker](http://fabric8.io/gitbook/docker.html#docker-containers) supported by Windows. 

We recommend using a linux based system for production. 

For development using Windows can be used but there are these known limitations.
Windows users may consider using a VM with a linux distribution for development.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.1 release, and will be removed in the 1.2 release.

The long term replacement is the [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) or if you wish to work with jars rather than bundles then use the [Java Container]((http://fabric8.io/gitbook/javaContainer.html)

