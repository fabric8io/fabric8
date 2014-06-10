## Java Container

The _Java Container_ is a way to provision and run a stand alone JVM process where you can define the exact classpath to be used, the main Java class, the java agent, JVM arguments and command line arguments. Its the ideal container for working with [Micro Services](http://fabric8.io/gitbook/microServices.html) since you have full control over exactly how much or how little middleware you wish to use; what versions of which developer frameworks and so forth.

You configure the Java container via the [io.fabric8.container.java.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.camel.spring.profile/io.fabric8.container.java.properties) file in your [profile](http://fabric8.io/gitbook/profiles.html). This is the profile configuration which is used by fabric8 to determine if the Java Container is to be used when creating a container for a profile.

Note that no application server or fabric8 jars are required to be inside the JVM. There is no Class Loader magic or strangeness going on. Literally its a flat classpath which you control the exact jars to be put on the classpath - thats it!

In fabric8 speak, we define a [profile](http://fabric8.io/gitbook/profiles.html) for each service; then we use fabric8 to create as many (java) containers as we need for the service. Fabric8 takes care of provisioning the containers and managing things. So fabric8 helps make Micro Services easy to use, manage and monitor.


## Example

For example take any Java maven archetype project which has an executable main of some kind. e.g. the Apache Camel spring archetype.

* create a fabric using the [latest distribution](http://fabric8.io/gitbook/getStarted.html)
* create the archetype and build it locally to check it works:

```
mvn archetype:generate -DarchetypeArtifactId=camel-archetype-spring -DarchetypeVersionId=2.13.0 -DarchetypeGroupId=org.apache.camel.archetypes
```

* enter **cool** for the groupId and **mydemo** for the artifact id
* run the following **[mvn fabric8:deploy](http://fabric8.io/gitbook/mavenPlugin.html)** goal to upload the maven project into fabric8 as a profile:

```
cd mydemo
mvn io.fabric8:fabric8-maven-plugin:1.1.0.CR1:deploy -Dfabric8.parentProfiles=containers-java.camel.spring
```

* In this particular case we specify the parent profile: **containers-java.camel.spring** which knows how to use a Java main() from the dependent camel/spring code in the project. Note that this parameter is currently optional; as the maven plugin tries its best to detect the correct parent profile to use; but its included for completeness.
* you should be able to see the new profile now in the wiki at [http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile)


### Using Child Containers

Now create a new container of the newly created **cool-mydemo** profile using the default **child** container provider.

e.g. navigate to the [cool-mydemo profile wiki page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile) and click the **New** button (on the top right) then enter a container name and click **Create Container**

Fabric8 will then create a new process for the container in the **processes/$containerName** folder in your fabric installation using the [Process Manager](http://fabric8.io/gitbook/processManager.html) capability.

Fabric8 will then copy all the jars defined in the maven project into the **processes/$containerName/lib** directory and startup the JVM using the **bin/launcher** command. The JVM comes with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM's [hawtio web console](http://hawt.io/)  to view its Camel routes or JMX MBeans etc.

Notice that the Java container works like any other fabric8 container; you can start it, stop it, view the container status page; see its type, provision status, provision list and so forth. Plus you can connect into it and see its [hawtio web console](http://hawt.io/) (such as Camel routes etc).

Whats really interesting is; the Class Path is specified completely by your projects pom.xml; that defines exactly what gets put into the lib directory. So if the Java works in a maven compile or test, it will work inside the fabric8 java container! No more Class Path hell :)

### Using Docker Containers

First you need to install [docker](https://www.docker.io/gettingstarted/#h_installation), setup the [environment variables](http://fabric8.io/gitbook/docker.html) and add the [docker profile](http://fabric8.io/gitbook/docker.html) to the root container so you can create docker containers in fabric8.

Now create a new container of the newly created **cool-mydemo** profile using the default **docker** container provider.

e.g. navigate to the [cool-mydemo profile wiki page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/cool/mydemo.profile) and click the **New** button (on the top right) then enter a container name and click **Create Container**

Fabric8 will then create a new docker container image from the base [java docker container](https://github.com/fabric8io/fabric8-java-docker) for your profile using the jars defined in the maven project and startup the JVM along with a [jolokia](http://jolokia.org/) java agent so that you can then connect into the JVM's [hawtio web console](http://hawt.io/) to view its Camel routes or JMX MBeans etc.

You should see in the logs of the root container how fabric8 will create a new docker image, copying the jars from fabric8's internal [maven repository](http://fabric8.io/gitbook/mavenProxy.html) into the docker container image. e.g. if you look at the images and containers tabs in the Docker tab in the console you should see the new image and the new container.

So with Docker each profile has its own docker image and each Java Container is a separate docker container.

If you want to poke around inside the new container image to see how it put jars into the lib directory create a new docker container using the image and bash. e.g. run a command like this on the command line where SOME_ID is the image id of the image that fabric8 created:

    docker run -i SOME_ID /bin/bash

#### Configuring the Docker image

The default java docker image works great; but you can use a different base image if, for example, you want to use a different unix/java installation or have some stuff already baked into the file system or lib directory etc.

To configure the docker base image just add a [io.fabric8.docker.provider.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.profile/io.fabric8.docker.provider.properties#L17) file to your profile and change the **image** property to specify which docker image you use to boot up the micro service.

### Configuring the Java Container

Fabric8 comes with a number of [java container profiles](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers) out of the box. If you want to configure how the java container works; such as to configure the main java class, change the java agent, JVM arguments or command line arguments, just add your own [io.fabric8.container.java.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.camel.spring.profile/io.fabric8.container.java.properties) file to your profile.

For example this [example io.fabric8.container.java.properties file](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/java.camel.spring.profile/io.fabric8.container.java.properties) specifies how to boot up the Java container main using camel and spring.

We'd love to have out of the box profiles for all the popular Java application frameworks which have a main Java bootstrap mechanism; such as CDI, Spring Boot, Vertx, DropWizard etc. If there's anything you think we're missing we love [contributions](http://fabric8.io/gitbook/contributing/index.html)!