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

### Building your docker image

To create a container on Kubernetes you need to create a docker image.

To enable the creation of a docker image use a maven plugin such as the [docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin/blob/master/README.md)

Then to build the docker image locally its

    mvn install docker:build

### Pushing your docker image

When running fabric8 on a number of machines your docker image needs to be in a docker registry so that it can be downloaded.

To push your image you will need to run:

    mvn install docker:build docker:push

If you wish to push docker images to a private or public registry you will need to add a section to your **~/.m2/settings.xml** file with a dummy login and password where the server **id** matches the value of **$DOCKER_REGISTRY**. For example a local boot2docker based registry would look like this:

```
	<servers>
       <server>
           <id>192.168.59.103:5000</id>
           <username>jolokia</username>
           <password>jolokia</password>
       </server>
        ...
  </servers>
```
       <server>
          <id>172.121.17.4:5000</id>
           <username>jolokia</username>
           <password>jolokia</password>
       </server>
        ...
  </servers>
```

For more details [see the docker maven plugin docs](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#authentication)

### Generating the JSON

An [App](apps.html) requires a **kubernetes.json** file which you can hand craft yourself; or you can put it into **src/main/resources** so that you can use Maven's default resource filtering to replace any project properties (e.g. the group ID, artifact ID, version number).

The **fabric8:json** goal will either copy or generate the JSON and then add it to your build so that the JSON gets released along with your artifacts.

    mvn fabric8:json
    cat target/classes/kubernetes.json

### Generating CDI environment variables

If you use CDI for your dependency injection and use the [@ConfigProperty](http://deltaspike.apache.org/documentation/configuration.html) annotation from [deltaspike](http://deltaspike.apache.org/) to inject environment variables or default values into your Java code then you can automatically generate a json schema file for each jar you create by just adding a **provided** scope dependency on the **fabric8-apt** module.

e.g. add this to your pom.xml

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>fabric8-apt</artifactId>
      <scope>provided</scope>
    </dependency>

This will then generate inside your jar a file called **io/fabric8/environment/schema.json** which will be a JSON Schema document describing all the environment variables, their types, default values and their description (if you added some javadoc for them).

#### Viewing all the environment variable injection points

If you have transitive dependencies which include the generated **io/fabric8/environment/schema.json** file in their jars you can view the overall list of environment variable injection points for a project via:

    mvn fabric8:describe-env

This will then list all the environment variables, their default value, type and description.


#### Including the environment variables in the generated JSON

By default any discovered environment variable JSON Schema files will be included in the generated JSON so that your [app JSON](apps.html) has all the available known environment variables from a CDI perspective; which makes it easy to change the app without too much detailed knownledge of the source code and helps reduce typeos since all the environment variable names are defaulted.

If you wish to disable this behaviour then set the maven property **fabric8.includeAllEnvironmentVariables** to false.

#### Properties for configuring the generation

You can use maven properties to customize the generation of the JSON:

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>docker.image</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the output docker image name.</td>
</tr>
<tr>
<td>fabric8.imagePullPolicy</td>
<td>Specifies the image pull policy; one of <code>PullAlways</code>,  <code>PullNever</code> or <code>PullIfNotPresent</code>, . Defaults to <code>PullAlways</code> if the project version ends with <code>SNAPSHOT</code> otherwise it is left blank. On newer OpenShift / Kubernetes versions a blank value implies <code>PullIfNotPresent</code></td>
</tr>
<tr>
<td>fabric8.replicationController.name</td>
<td>The name of the replication controller used in the generated JSON.</td>
</tr>
<tr>
<td>fabric8.container.name</td>
<td>The docker container name of the application in the generated JSON.</td>
</tr>
<tr>
<td>fabric8.generateJson</td>
<td>If set to false then the generation of the JSON is disabled.</td>
</tr>
<tr>
<td>fabric8.includeAllEnvironmentVariables</td>
<td>Should the environment variable JSON Schema files, generate by the **fabric-apt** API plugin be discovered and included in the generated kuberentes JSON file. Defaults to true.</td>
</tr>
<tr>
<td>fabric8.json.template</td>
<td>The name of the <a href="http://mvel.codehaus.org/">MVEL</a> template found on the classpath to use for the generation of the JSON.</td>
</tr>
<tr>
<td>fabric8.replicas</td>
<td>The number of pods to create for the <a href="http://fabric8.io/guide/replicationControllers.html">Replication Controller</a> if the plugin is generating the App JSON file.</td>
</tr>
<tr>
<td>fabric8.service.name</td>
<td>The name of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.port</td>
<td>The port of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.containerPort</td>
<td>The container port of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.protocol</td>
<td>The protocol of the service. (If not specified then kubernetes will default it to TCP).</td>
</tr>
<tr>
<td>fabric8.service.&lt;portName&gt;.port</td>
<td>The service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;portName&gt;.containerPort</td>
<td>The container port to target to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;portName&gt;.protocol</td>
<td>The protocol of this service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;portName&gt;containerPort</td>
<td>The container port of the Service to generate (if a kubernetes service is required).</td>
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
<td>docker.image</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the output docker image name.</td>
</tr>
<tr>
<td>docker.registry</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the local docker registry to use (if not using the public docker registry).</td>
</tr>
</table>


