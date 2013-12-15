## Maven Proxy

In a lot of cases people will run Fabric8 inside an environment with limited or no access at all to the internet.
The fabric-agent will still need to download artifacts from somewhere. Forcing the users to use a 3rd party maven repository manager doesn't seem like a good idea.
So Fabric8 provides a lightweight maven repository manager that provides the ability of uploading & downloading maven artifacts inside a fabric.

### Installation

When creating a new fabric using commands like:

      fabric:create

the container will automatically install the fabric-maven-proxy feature on the current container. Additionally, the user can have a fabric-maven-proxy run on additional containers by adding the fabric-maven-porxy feature in one of their profiles. By default this feature is part of the fabric profile.

### Discovery

To avoid having the fabric-maven-proxy as a single point of failure fabric can discover & use any fabric-maven-proxy that is running inside the fabric cluster. Each container running the fabric-maven-proxy will register it in the runtime registry and the fabric-agent will be able to discover it from there.
When having multiple fabric-maven-proxies running, the fabric-agent will use them in the same order that were added in the runtime registry.

## Configuration
Out of the box any fabric-maven-proxy will make use of a default set of remote and local repositories.

#### Local repository
Local repository is a read-write repository, which takes priority over the remote repositories. Any artifact lookup, will first use the local repository and if the target artifact is not found the remote repositories will be used.
Moreover, any artifact that was found in the remote repositories it will be also copied to the local repository.

The default local repository is the users maven repository is data/maven/proxy/downloads under the installation directory.

#### Remote repositories
Any read-only repository can be configured as remote repository.

#### Configuring repositories
Both remote and local repositories can be configured using the io.fabric8.maven pid. That configuration accepts two properties:

*localRepository*  The path to the local repository, defaults to data/maven/proxy/downloads

*remoteRepositories* The remote repositories as a comma separated list.

Here is an example of how you can change the local repository for a single fabric-maven-proxy.

      config:propset --pid io.fabric8.maven localRepository /path/to/my/repo myprofile

Fabric maven proxies that are running on managed containers, are configured via fabric profiles. So this would look like:

      fabric:profile-edit --pid io.fabric8.maven/localRepository=/path/to/my/repo myprofile

In the last example there are two things that you need to take into consideration. The firs thing is that not necessarily all containers are managed.
The second is that not all containers running the fabric-maven-proxy use necessarily the same profiles, so its best to make the change in the profile that is the least common denominator *(e.g the default profile)*.


### Deploying artifacts to a fabric-maven-proxy

As already mentioned above, the fabric-maven-proxy allows you to deploy artifacts to it, so that it can be used for provisioning.
Assuming that the fabric-maven-proxy is running on *exampleHost*, the http url for uploading will be *http://username:password@exampleHost:8181/maven/upload*.
The username and password are the credentials of any user with the admin role. Note that the role is also configurable inside the io.fabric8.maven pid.

#### Use the fabric-maven-proxy with mvn from the shell

You can specify the url to the fabric-maven-proxy directly from the shell for deploying any maven project to the project.

      mvn deploy -DaltDeploymentRepository=http://username:password@exampleHost:8181/maven/upload

an alternative for deploying a single file as a maven artifact:

      mvn deploy:deploy-file -Dfile=/path/to/target/file.jar -DartifactId=myartifactId -DgroupId=mygroupId -Dversion=1.0.0 -Dtype=jar  -Durl=http://username:password@exampleHost:8181/maven/upload

#### Configure the fabric-maven-proxy in the user maven settings.xml

To avoid using the fabric-maven-proxy url each time you want to deploy an artifact to fabric, you can configure the fabric-maven-proxy inside the maven settings.xml.


The first step is to add inside the maven project a distribution management element that will point to the fabric-maven-proxy.

<distributionManagement>
     <repository>
        <id>my-fabric-maven-proxy</id>
        <name>Fabric Maven Proxy</name>
        <url>http://exampleHost:8181/maven/upload</url>
      </repository>
    </distributionManagement>


The second step is to configure maven by with the credentials for the maven proxy, by adding a new server element in the settings.xml

    <server>
        <id>my-fabric-maven-proxy-id</id>
        <username>username</username>
        <password>username</password>
    </server>

### Integrating with 3rd party maven repository managers
If it hasn't been clear yet, you can integrate with a 3rd party maven repository manager either by adding them to the list of repositories in the fabric-agent configuration, or by adding them in the list of remote repositories of the fabric-maven-proxy.

The question is when to do go by the first approach and when to go by the second approach.

The actual difference in the two approaches is that the fabric-maven-proxy will store any artifact it fetches from remote repos to its local repository. Future requests for that such artifacts will be served using the local repository.
If retrieving artifacts from the 3rd party repository manager is an expensive operation, then you definitely need to use the fabric-maven-proxy, as it will limit requests to the 3rd party repository manager.



