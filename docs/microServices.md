## Micro Services

There is a lot of attention on Micro Services these days. From our perspective this is about creating a stand alone process which is self contained and as such does not need a traditional Java Application Server. You just choose whatever jars go onto the static flat classpath and the micro service starts up so that the micro service JVM has as little java code as possible; just enough to function but not including a whole application server.

In fabric8 a _Java Micro Service_ is any stand alone JVM process which you, the user, can define the exact classpath to be used. No application server or fabric8 plugins are required to be inside the JVM. There is no ClassLoader magic or strangeness going on. Literally its a flat classpath which you control the exact jars to be put on the classpath - thats it!

### Example

For example take any Java maven archetype project which has an executable main of some kind. e.g. the Apache Camel spring archetype.

* create a fabric using the [latest distribution](http://fabric8.io/#/site/book/doc/index.md?chapter=getStarted_md
) and make sure you've added the [docker profile](http://fabric8.io/#/site/book/doc/index.md?chapter=docker_md) so you can create docker containers
* create the archetype and build it locally to check it works:

```
mvn archetype:generate -DarchetypeArtifactId=camel-archetype-spring -DarchetypeVersionId=2.13.0 -DarchetypeGroupId=org.apache.camel.archetypes
```

* enter **cool** for the groupId and **mydemo** for the artifact id
* run the following **mvn fabric8:deploy** goal to upload the maven project into fabric8 as a profile:

```
cd mydemo
mvn io.fabric8:fabric8-maven-plugin:1.1.0.Beta4:deploy -Dfabric8.parentProfiles=containers-java.camel.spring
```

In this particular case its using the **containers-java.camel.spring** profile which knows how to use a Java main from the dependent camel/spring code in the project.

Now create an instance of the newly created **cool-mydemo** profile using the default **docker** container provider. Fabric8 will then create a new container image for your profile using the jars defined in the maven project and startup the JVM along with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM to view its Camel routes or JMX MBeans etc.

You should see in the logs of the root container how fabric8 will create a new image, copying the jars from fabric's maven repository into the docker container image. e.g. if you look at the images and containers tabs in the Docker tab in the console you should see the new image and the new container.

If you want to poke around inside the new container image to see how it put jars into the lib directory create a new docker container using the image and bash. e.g. run a command like this on the command line where SOME_ID is the image id of the image that fabric8 created:

    docker run -i SOME_ID /bin/bash

Whats really interesting is; the ClassPath is specified completely by your projects pom.xml; that defines exactly what gets put into the lib directory. So if the Java works in a maven compile, it will work inside the fabric8 micro container (which is just a simple [docker container](https://github.com/fabric8io/fabric8-java-docker)).

Also you could customize your own docker container image; having some stuff already baked into the file system or lib directory. e.g. clone the [containers-java profile](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.profile) and override the [image name in the io.fabric8.docker.provider.properties file](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.profile/io.fabric8.docker.provider.properties#L18) to specify which docker image you use to boot up the micro service.


