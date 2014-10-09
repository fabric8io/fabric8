## Fabric8 Maven Plugin

This maven plugin makes it easy to create or deploy [Apps](apps.html) as part of your maven project.

### Specifying the location of your local docker registry

When creating new docker images for use in Kubernetes you probably want to run a local docker registry if you do not intend to reuse the public docker registry.

Make sure you have added this to your **~/.m2/settings.xml** file to define the **docker.registry** value in a profile you can activate by default...

e.g. add this to the &lt;servers&gt; element:

    <?xml version="1.0"?>
    <settings>

      <profiles>
        <profile>
          <id>docker-host</id>
          <properties>
            <docker.registry>${env.DOCKER_REGISTRY}</docker.registry>
          </properties>
        </profile>
      </profiles>

      ...

      <activeProfiles>
        <activeProfile>docker-host</activeProfile>
      </activeProfiles>
    </settings>

### Adding the plugin to your project

To enable this maven plugin and to automatically release the [App JSON file](apps.html) as part of your build add this to your pom.xml:

      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>${fabric.version}</version>
        <executions>
          <execution>
            <id>attach-json</id>
            <goals>
              <goal>attach-json</goal>
            </goals>
          </execution>
        </executions>
      </plugin>


### Building and Pushing your docker image

To create a container on Kubernetes you need to create a docker image.

To enable the creation of a docker image use a maven plugin such as the [docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin/blob/master/README.md)

Then to build the docker image locally its

    mvn install docker:build

Or to push the docker image to your local docker registry

    mvn install docker:push

**Note** if the above fails (we have seen it sometimes fail), perform a docker:build instead then use the command line:

    docker push $DOCKER_REGISTRY/mydemo/war-camel-servlet:2.0.0-SNAPSHOT

### Deploying

Before deploying you need to make sure your docker image is available to Kubernetes. See above for how to do this. Any docker registry that is accessible to Kubernetes is supported.

To deploy your container use the following goal:

    mvn fabric8:deploy

This defaults to using the [App JSON file](apps.html) file located at **src/main/resources/kubernetes.json** so that it can be filtered like most maven resources are; to substitute docker image name and version etc.


### Example

The following example shows you how to build and push a docker image to Kubernetes and deploy it and then use it.

Make sure you have [installed OpenShift and Fabric8](http://fabric8.io/v2/getStarted.html) (which installs the web console and the local docker registry).

You should be able to check if the docker registry is running OK via this command (which should return 'true'):

    curl http://$DOCKER_REGISTRY/v1/_ping

Now you are ready to build a quickstart!

#### Build the camel-servlet web application

From the distribution or source code perform these commands to push the docker image:

    cd quickstarts/war/camel-servlet
    mvn clean install docker:build
    docker push $DOCKER_REGISTRY/mydemo/war-camel-servlet:2.0.0-SNAPSHOT

Now lets deploy the image into the Kubernetes environment:

    mvn fabric8:deploy

You should now be able to view the running web application at http://dockerhost:9901/war-camel-servlet-2.0.0-SNAPSHOT/

### Property Reference

The following maven property values are used to configure the behaviour of the maven plugin:

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>docker.dataImage</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the output docker image name.</td>
</tr>
<tr>
<td>docker.registry</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the local docker registry to use (if not using the public docker registry).</td>
</tr>

<!--
<tr>
<td>fabric8.replicas</td>
<td>The number of pods to create for the <a href="http://fabric8.io/v2/replicationControllers.html">Replication Controller</a> if the plugin is generating the App JSON file.</td>
</tr>
</table>


