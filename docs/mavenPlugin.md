## Fabric8 Maven Plugin

This maven plugin makes it easy to work with Kubernetes from your Maven project.

### Goals

For building and pushing docker images

* [docker:build](mavenDockerBuild.html) builds the docker image for your maven project
* [docker:push](mavenDockerPush.html) pushes the locally built docker image to the global or a local docker registry

For generating and applying Kubernetes JSON

* [fabric8:json](mavenFabric8Json.html) generates kubernetes json for your maven project
* [fabric8:apply](mavenFabric8Apply.html) applies the kubernetes json into a namespace in a kubernetes cluster

Helper goals for working with Kubernetes 

* [fabric8:create-env](mavenFabric8CreateEnv.html) generates environment variable scripts for Kubernetes [services](services.html) so you can simulate running programs as if they were inside kubernetes
* [fabric8:create-routes](mavenFabric8CreateRoutes.html) generates any missing [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) for the current services 
* [fabric8:delete-pods](mavenFabric8DeletePods.html) deletes pods for the current projects docker image so that they get recreated by the [replication controllers](replicationControllers.html) to use the latest image

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


### Generating CDI environment variables

If you use CDI for your dependency injection and use the [@ConfigProperty](http://deltaspike.apache.org/documentation/configuration.html) annotation from [deltaspike](http://deltaspike.apache.org/) to inject environment variables or default values into your Java code then you can automatically generate a json schema file for each jar you create by just adding a **provided** scope dependency on the **fabric8-apt** module.

e.g. add this to your pom.xml

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>fabric8-apt</artifactId>
      <scope>provided</scope>
    </dependency>

This will then generate inside your jar a file called **io/fabric8/environment/schema.json** which will be a JSON Schema document describing all the environment variables, their types, default values and their description (if you added some javadoc for them).

#### Including the environment variables in the generated JSON

By default any discovered environment variable JSON Schema files will be included in the generated JSON so that your [app JSON](apps.html) has all the available known environment variables from a CDI perspective; which makes it easy to change the app without too much detailed knowledge of the source code and helps reduce typeos since all the environment variable names are defaulted.

If you wish to disable this behaviour then set the maven property **fabric8.includeAllEnvironmentVariables** to false.

#### Viewing all the environment variable injection points

If you have transitive dependencies which include the generated **io/fabric8/environment/schema.json** file in their jars you can view the overall list of environment variable injection points for a project via:

    mvn fabric8:describe-env

This will then list all the environment variables, their default value, type and description.


### Deploying into the Library

Before deploying you need to make sure your docker image is available to Kubernetes. See above for how to do this. Any docker registry that is accessible to Kubernetes is supported.

To deploy your [App Zip](appzip.html) into the wiki in the web console use the following goal:

    mvn fabric8:deploy

This goal uses the default fabric8 console URL of **http://dockerhost:8484/hawtio/** unless you specify the FABRIC8_CONSOLE environment variable to point at something else.

e.g. to try this against a locally running hawtio try:

    export FABRIC8_CONSOLE=http://localhost:8282/hawtio/


This goal will then POST the [App Zip](appzip.html) into the wiki so you should be able to view the newly posted [App](apps.html) at [http://dockerhost:8484/hawtio/wiki/branch/master/view](http://dockerhost:8484/hawtio/wiki/branch/master/view)

### Example

To see these plugins in action check out how to [run and example quickstart](example.html)



