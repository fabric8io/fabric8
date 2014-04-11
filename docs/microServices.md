## Micro Services

There is a lot of attention on Micro Services these days. From our perspective this is about creating a stand alone process which is self contained and as such does not need a traditional Java Application Server. You just choose whatever jars go onto the static, flat classpath and the micro service starts up.

In fabric8 a _Java Micro Service_ is any stand alone JVM process which you, the user, can define the exact classpath to be used. No application server or fabric8 plugins are required to be inside the JVM. There is no ClassLoader magic or strangeness going on. Literally its a flat classpath which you control - thats it!

### Example

For example take any Java maven archetype project which has an executable main of some kind. e.g. an Apache Camel archetype.

* create the archetype and build it locally to check it works
* run the following **mvn fabric8:deploy** goal to upload the maven project into fabric8 as a profile:

    mvn io.fabric8:fabric8-maven-plugin:1.1.0-SNAPSHOT:deploy -Dfabric8.profile=mydemo -Dfabric8.parentProfiles=containers-docker-java.camel.spring

In this particular case its using the **containers-docker-java.camel.spring** profile which knows how to use a Java main from the dependent camel/spring code in the project.

Now create an instance of the newly creaated **mydemo** profile using the default **docker** container provider. Fabric8 will then create a new container image for your profile using the jars defined in your maven project and startup the JVM along with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM to view its Camel routes or JMX MBeans etc.

Whats really interesting is; the ClassPath is specified completely by your projects pom.xml; so if the Java works in a maven compile, it will work inside the fabric8 micro container (which is just a simple [docker container](https://github.com/fabric8io/fabric8-java-docker))


