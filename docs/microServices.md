## Micro Services

There is a lot of attention on Micro Services these days. From our perspective this is about creating a stand alone process which is self contained and as such does not need a traditional Java Application Server. You just choose whatever jars go onto the static flat classpath and the micro service starts up so that the micro service JVM has as little java code as possible; just enough to function but not including a whole application server.

In fabric8 we implement Micro Services with the _Java Container_ which is a stand alone JVM process which you  can define the exact classpath to be used. No application server or fabric8 plugins are required to be inside the JVM. There is no ClassLoader magic or strangeness going on. Literally its a flat classpath which you control the exact jars to be put on the classpath - thats it!

The easiest application server to work with in the world is literally a flat class path :)

## Example

For example take any Java maven archetype project which has an executable main of some kind. e.g. the Apache Camel spring archetype.

* create a fabric using the [latest distribution](http://fabric8.io/#/site/book/doc/index.md?chapter=getStarted_md
)
* create the archetype and build it locally to check it works:

```
mvn archetype:generate -DarchetypeArtifactId=camel-archetype-spring -DarchetypeVersionId=2.13.0 -DarchetypeGroupId=org.apache.camel.archetypes
```

* enter **cool** for the groupId and **mydemo** for the artifact id
* run the following **[mvn fabric8:deploy](http://fabric8.io/#/site/book/doc/index.md?chapter=mavenPlugin_md)** goal to upload the maven project into fabric8 as a profile:

```
cd mydemo
mvn io.fabric8:fabric8-maven-plugin:1.1.0.Beta4:deploy -Dfabric8.parentProfiles=containers-java.camel.spring
```

* In this particular case its using the **containers-java.camel.spring** profile which knows how to use a Java main from the dependent camel/spring code in the project.
* you should be able to see the new profile now in the wiki at [http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile)


### Using Child Containers

Now create a new container of the newly created **cool-mydemo** profile using the default **child** container provider.

Fabric8 will then create a new process for the container in the **processes/$containerName** folder in your fabric installation using the [Process Manager](http://fabric8.io/#/site/book/doc/index.md?chapter=processManager_md) capability.

Fabric8 will then copy all the jars defined in the maven project into the **processes/$containerName/lib** directory and startup the JVM using the **bin/launcher** command. The JVM comes with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM to view its Camel routes or JMX MBeans etc.

Notice that the Java container works like any other container; you can start it, stop it, view the container status page; see its type, provision status, provision list and so forth. Plus you can connect into it and see its [hawtio web console](http://hawt.io/) (such as Camel routes etc).

Whats really interesting is; the ClassPath is specified completely by your projects pom.xml; that defines exactly what gets put into the lib directory. So if the Java works in a maven compile, it will work inside the fabric8 java container! No more ClassPath hell :)

### Using Docker Containers

First you need to install [docker](https://www.docker.io/gettingstarted/#h_installation), setup the [environment variables](http://fabric8.io/#/site/book/doc/index.md?chapter=docker_md) and add the [docker profile](http://fabric8.io/#/site/book/doc/index.md?chapter=docker_md) to the root container so you can create docker containers in fabric8.

Now create a new container of the newly created **cool-mydemo** profile using the default **docker** container provider.

Fabric8 will then create a new docker container image from the base [java docker container](https://github.com/fabric8io/fabric8-java-docker) for your profile using the jars defined in the maven project and startup the JVM along with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM to view its Camel routes or JMX MBeans etc.

You should see in the logs of the root container how fabric8 will create a new docker image, copying the jars from fabric's maven repository into the docker container image. e.g. if you look at the images and containers tabs in the Docker tab in the console you should see the new image and the new container.

So with Docker each profile has its own docker image and each Java Container is a separate docker container.

If you want to poke around inside the new container image to see how it put jars into the lib directory create a new docker container using the image and bash. e.g. run a command like this on the command line where SOME_ID is the image id of the image that fabric8 created:

    docker run -i SOME_ID /bin/bash

#### Configuring the Docker image

The default java docker image works great; but you can use a different base image if, for example, you want to use a different unix/java installation or have some stuff already baked into the file system or lib directory etc.

To configure the docker base image just add a [io.fabric8.docker.provider.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.profile/io.fabric8.docker.provider.properties#L17) file to your profile and change the **image** property to specify which docker image you use to boot up the micro service.

### Configuring the Java Container

Fabric8 comes with a number of [java container profiles](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers) out of the box. If you want to configure how the java container works; such as to configure the main java class, change the java agent, JVM arguments or command line arguments, just add your own [io.fabric8.container.java.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.camel.spring.profile/io.fabric8.container.java.properties) file to your profile.

For example this [example io.fabric8.container.java.properties file](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.camel.spring.profile/io.fabric8.container.java.properties) specifies how to boot up the Java container main using camel and spring.

We'd love to have profiles for all the popular Java application frameworks which have a main Java bootstrap mechanism; such as CDI, Spring Boot, Vertx, DropWizard etc. If there's anything you think we're missing we love [contributions](http://fabric8.io/#/site/Contributing.md)!