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
            <docker.url>${env.DOCKER_HOST}</docker.url>
          </properties>
        </profile>
      </profiles>

      ...

      <activeProfiles>
        <activeProfile>docker-host</activeProfile>
      </activeProfiles>
    </settings>

### Adding the plugin to your project

To enable this maven plugin and to automatically generate/copy and release the [App JSON file](apps.html) as part of your build add this to your pom.xml:

      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>${fabric.version}</version>
        <executions>
          <execution>
            <id>json</id>
            <goals>
              <goal>json</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

To automatically generate an [App Zip](appzip.html) for your project then add this:

      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <version>${project.version}</version>
        <executions>
          <execution>
            <id>zip</id>
            <phase>package</phase>
            <goals>
              <goal>zip</goal>
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

    docker push $DOCKER_REGISTRY/quickstart/war-camel-servlet:2.0-SNAPSHOT

### Generating the JSON

An [App](apps.html) requires a **kubernetes.json** file which you can hand craft yourself; or you can put it into **src/main/resources** so that you can use Maven's default resource filtering to replace any project properties (e.g. the group ID, artifact ID, version number).

The **fabric8:json** goal will either copy or generate the JSON and then add it to your build so that the JSON gets released along with your artifacts.

    mvn fabric8:json
    cat target/classes/kubernetes.json

#### Properties for configuring the generation

You can use maven properties to customize the generation of the JSON:

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
<td>fabric8.kubernetes.name</td>
<td>The name of the application used in the id of the JSON and as a label.</td>
</tr>
<tr>
<td>fabric8.kubernetes.kubernetesContainerName</td>
<td>The docker container name of the application; if undefined it uses the lower case un-camelcased name.</td>
</tr>
<tr>
<td>fabric8.generateJson</td>
<td>If set to false then the generation of the JSON is disabled.</td>
</tr>
<tr>
<td>fabric8.json.template</td>
<td>The name of the <a href="http://mvel.codehaus.org/">MVEL</a> template found on the classpath to use for the generation of the JSON.</td>
</tr>
<tr>
<td>fabric8.replicas</td>
<td>The number of pods to create for the <a href="http://fabric8.io/v2/replicationControllers.html">Replication Controller</a> if the plugin is generating the App JSON file.</td>
</tr>
<tr>
<td>fabric8.env.FOO = BAR</td>
<td>Defines the environment variable FOO and value BAR.</td>
</tr>
<tr>
<td>fabric8.label.FOO = BAR</td>
<td>Defines the kubernetes label FOO and value BAR.</td>
</tr>
<tr>
<td>fabric8.port.container.FOO = 1234</td>
<td>Defines the port named FOO has a container port 1234.</td>
</tr>
<tr>
<td>fabric8.port.host.FOO = 4567</td>
<td>Defines the port named FOO has a host port 4567.</td>
</tr>
</table>


### Deploying

Before deploying you need to make sure your docker image is available to Kubernetes. See above for how to do this. Any docker registry that is accessible to Kubernetes is supported.

To deploy your [App Zip](appzip.html) into the wiki in the web console use the following goal:

    mvn fabric8:deploy

This goal uses the default fabric8 console URL of **http://dockerhost:8484/hawtio/** unless you specify the FABRIC8_CONSOLE environment variable to point at something else.

e.g. to try this against a locally running hawtio try:

    export FABRIC8_CONSOLE=http://localhost:8282/hawtio/


This goal will then POST the [App Zip](appzip.html) into the wiki so you should be able to view the newly posted [App](apps.html) at [http://dockerhost:8484/hawtio/wiki/branch/master/view](http://dockerhost:8484/hawtio/wiki/branch/master/view)

### Running

Before running you need to make sure your docker image is available to Kubernetes. See above for how to do this. Any docker registry that is accessible to Kubernetes is supported.

To run your App use the following goal:

    mvn fabric8:run

This defaults to using the [App JSON file](apps.html) file located at **src/main/resources/kubernetes.json** so that it can be filtered like most maven resources are; to substitute docker image name and version etc.

### Example

To see these plugins in action check out how to [run and example quickstart](example.html)

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
</table>


