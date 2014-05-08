## Micro Services

There's lots of heated debate of late on micro services and what they are. From our perspective its simply about about creating separate (JVM) processes using the [Java Container](http://fabric8.io/#/site/book/doc/index.md?chapter=javaContainer_md) for each service so that each service is self contained, isolated, managed and has a minimal footprint.

You just choose whatever jars go onto the static flat classpath; just enough to function well but not necessarily including a whole Application Server (though you are free to embed whatever libraries and frameworks you need including embedding whatever parts of, say, [Apache Tomcat](http://tomcat.apache.org/), [Apache Karaf](http://karaf.apache.org/) or [WildFly](http://wildfly.org/) you wish to use).

From our perspective the main benefits of Micro Services are:

* Simplicity
* Minimal Footprint
* Process Isolation

#### Simplicity

With Micro Services you specify the exact list of jars to be on the classpath; thats it. No magic Class Loaders; no complex graph of Class Loader trees to understand or complex OSGi package level versioned import/export statements. 

A simple, flat classpath (jars in a lib directory) thats very simple to understand.  

No more fighting with ClassCastException because you have 2 versions of a given class in different parts of the class loader tree, or ClassNotFoundException if one branch of your Class Loader tree can't see another branch. No fighting with Application Server internals or clashes with jaxb or logging libraries included in the Application Server. You just pick the jars you need.

Wondering whats on your classpath? Just look in the lib directory. The easiest Application Server to work with in the world is literally a flat class path :). Simples!

#### Minimal Footprint

Only include the exact list of jars you need to implement the Micro Service. Be as minimal as you want or need to be :)

That way your JVM uses the least amount of memory, threads, file descriptors and IO. This also leads to the fastest possible startup of your service. 

#### Process Isolation

Rather than running all your services in the same JVM; you run separate JVM processes for each micro service. This has many benefits:

* **easy to monitor and manage**: on any machine you can run tools like **ps** and **top** or other task/activity monitors to see which services are using up the RAM, CPU, IO or Network
* **fine grained changes**: its easy to stop, upgrade and restart a micro service without affecting any other services. e.g. to upgrade a version of, say, Apache Camel; you don't need to disturb other services; you can just update a specific service process leaving the other services alone. This also avoids the big bang upgrade issues if you wish to upgrade the JDK or Application Server version; instead you can migrate versions on a per micro service basis as and when a service is ready to upgrade; you don't have to wait for all services to be upgradeable.
* **efficient scaling**: its easy to auto-scale individual micro services without wasting resources on services you don't need to scale
* **easy multi-tenancy**: if you use Docker, SELinux or OpenShift you can specify CPU, memory, disk and IO limits on each process plus put each process into separate security groups to keep them completely isolated when required.
* **undeploy that works**: few folks in production ever risk undeploying anything in an Application Server for fear of resource, thread, connection, memory or file descriptor leaks. Instead folks deploy new Application Servers with the new stuff and not the old stuff; then phase out the old stuff. With micro services you can do the same thing; but at the fine grained Micro Service level; start the new services; phase out the old ones - but leave all the other services untouched to avoid unnecessary restarts.
* **easy leak detection and workarounds**: if a service has a resource, memory or thread leak, its easy to pinpoint which service has the problem (and perform automatic reboots until the issue is resolved).
* **fine grained logs and monitoring**: each micro service gets its own directory where logs and other files are generated. Its also easier to monitor different micro services differently (e.g. different poll rates or metrics collected).

It must be said that using more processes can use more memory and resources but we feel the benefits greatly outweigh the costs; particularly as memory and disk getting cheaper while people's time remains a scarce commodity and agility is of the essence. Using more processes are harder to manage in theory; though fabric8 helps make that easy via its use of profiles and containers.

### How to use Micro Services in Fabric8

To implement Micro Services in Fabric8 we use the [Java Container](http://fabric8.io/#/site/book/doc/index.md?chapter=javaContainer_md) to run each service as a completely separate and isolated JVM process. This lets us start, stop and perform [rolling upgrades](http://fabric8.io/#/site/book/doc/index.md?chapter=rollingUpgrade_md) to service instances without affecting other services.

We create a [Profile](http://fabric8.io/#/site/book/doc/index.md?chapter=profiles_md) for each Micro Service so that its easy to view the entire fabric and all its containers; grouping them by profile (or service).

To help auto-scale individual micro services we can then use the [Profile Requirements](http://fabric8.io/#/site/book/doc/index.md?chapter=requirements_md) to define the sizing requirements and automatically scale up and down using the JMX API calls on Fabric8 in an operational management tool's [alerting mechanism](https://docs.jboss.org/author/display/RHQ/Alerts) like [JBoss RHQ](http://rhq.jboss.org/)



