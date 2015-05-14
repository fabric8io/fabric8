## docker:push

The maven `fabric8:push` goal pushes the docker image generated via [mvn docker:build](mavenDockerBuild.html) to either the global docker registry or a local docker registry.

When running kubernetes on a number of machines your docker image needs to be in a docker registry so that it can be downloaded and cached on each host.

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

### Reference

See the [reference docs for docker:push](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#dockerpush)