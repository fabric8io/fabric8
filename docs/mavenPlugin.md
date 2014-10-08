## Fabric8 Maven Plugin

This maven plugin makes it easy to create or deploy [Apps](apps.html) as part of your maven project.

### Specifying the location of your local docker registry

When creating new docker images for use in Kubernetes you probably want to run a local docker registry if you do not intend to reuse the public docker registry.

Its a good idea to define an environment variable to point to your docker registry:

    export DOCKER_REGISTRY=172.17.0.21:5000


Then configure this: edit your **~/.m2/settings.xml** file to define the **docker.registry** value in a profile you can activate by default...

e.g. add this to the &lt;servers&gt; element:

    <?xml version="1.0"?>
    <settings>

      <profiles>
        <profile>
          <id>docker-host</id>
          <properties>
            <docker.registry>172.17.0.21:5000</docker.registry>
            <docker.url>http://192.168.59.103:2375</docker.url>
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

First make sure you've defined an environment variable to point to your docker registry:

    export DOCKER_REGISTRY=172.17.0.21:5000

Make sure you are running the registry. e.g. to create one on OpenShift type:

    cd fabric8/apps
    openshift kube apply -c registry-config.json

You should be able to check if the registry is running OK via this command (which should return 'true'):

    curl $DOCKER_REGISTRY/v1/_ping

Now you are ready to build your container.

From the distribution or source code perform these commands to push the docker image:

    cd quickstarts/war/camel-servlet
    mvn clean install docker:build
    docker push $DOCKER_REGISTRY/mydemo/war-camel-servlet:2.0.0-SNAPSHOT

Now lets deploy the image into the Kubernetes environment:

    mvn fabric8:deploy

You should now be able to view the web application at http://dockerhost:9901/war-camel-servlet-2.0.0-SNAPSHOT/ where 'dockerhost' should point to the ip address returned by

    boot2docker ip

If you are not running docker natively on linux. (Or just use localhost if you are). [This article](http://viget.com/extend/how-to-use-docker-on-os-x-the-missing-guide) describes how its a good idea to define **dockerhost** to point to your boot2docker ip address:

    echo $(docker-ip) dockerhost | sudo tee -a /etc/hosts

Then you can easily access anything running on your docker host without having to setup port forwarding.


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


