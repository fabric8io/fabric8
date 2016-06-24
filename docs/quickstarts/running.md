### Using quickstarts

First, make sure you have followed the [Get Started Guide](../getStarted/index.md)
so you should have things running and you have
[setup your local machine](../getStarted/local.md).

Please also check out
[how to develop applications locally](../getStarted/develop.md) that you have your
local tools available.

You can run one of the [quickstarts](index.md) either directly out of
a git checked out repository or from a project created by an
quickstart [archetype](archetype.md).

The quickstarts can be used directly by

    git clone https://github.com/fabric8io/ipaas-quickstarts.git
    cd quickstart

and then into the subdirectory of a specific quickstart.

For the rest of this chapter we are using `cdi/camel` as an example
and assume that you are within its directory.

#### Check your environment

In order to build docker images you must have access to a Docker
daemon. The easiest way is to setup the environment variable
`DOCKER_HOST`. Alternatively you can use the Maven property
`docker.host` to point to the Docker daemon.

Also for applying to OpenShift / Kubernetes, your `KUBERNETES_MASTER`
and `KUBERNETES_NAME` environment variables should be setup
properly. Or, alternatively, login into OpenShift with `oc login`. See
the documentation of [`fabric8:apply`](../mavenFabric8Apply.md) for
details. 

#### Build the application and the Docker image

The Docker image can be easily created by using the following goals:

    mvn clean install docker:build

For you convenience, these goals are combined by using a
pre-configured goal:

    mvn -Pf8-build

Please note that by default the Docker username is "fabric8" and the
default registry is "docker.io". This works by default when you are
not pushing to a registry with `docker:push`. Please see below how you
can customize this to your needs

#### Deploy the application on Kubernetes / OpenShift

Now let's deploy the image into the Kubernetes environment:

    mvn fabric8:json fabric8:apply

Alternatively you can use the shortcut

    mvn -Pf8-local-deploy

which will include the install and `docker:build` steps. This works
nicely when you are using the
[Fabric8 Vagrant Image](../getStarted/vagrant.md) with its one node
setup. When you are running on a full OpenShift cluster, you should
use

    mvn docker:push fabric8:json fabric8:apply

or 

    mvn -Pf8-deploy

to include a `docker:push` to the registry. However, for this to work
you need to properly configure the username and registry (see below).

You should now be able to view the quickstart in the fabric8 console.
On the Services tab you will see the camel-servlet URL which will take
you to the running example. 

### Changing Docker user and registry

As said above, the quickstarts use an image user **fabric8** and as
registry the default `index.docker.io`. Obviously you won't be able to
push this image to `index.docker.io` because this must be done with
permissions for the account `fabric8`.
 
If you want to push the image to you own account on Docker hub, use
the option `-Dfabric8.dockerUser` to specify your username:
 
    mvn clean install docker:build docker:push -Dfabric8.dockerUser=morlock/

(Pleade note the trailing `/` after the username). Authentication for this user *morlock* must 
be done as described in the [manual for the docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin). 
E.g. you can use `-Ddocker.push.username` and `-Ddocker.push.password` for specifying the
credentials or you can set this up in your `~/.m2/settings.xml`.
 
Alternatively you can push the image also to another registry, like
the OpenShift internal registry. Assuming that you use the
[fabric8 Vagrant image](../getStarted/vagrant.md)
and have set up the routes properly, the OpenShift registry is
available as `docker-registry.vagrant.f8`. If your OpenShift user
is authenticated against Docker as desribed in the
[OpenShift Documentation](https://docs.openshift.com/enterprise/3.0/install_config/install/docker_registry.html#access)
and a project **fabric8** exists (`oc new-project fabric8` if
required), then you can push to this registry with
 
    mvn clean install docker:build docker:push -Ddocker.push.registry=docker-registry.vagrant.f8 \
                                               -Ddocker.push.useOpenShiftAuth
 
These properties can be used also with the shortcut profiles `f8-build`, `f8-deploy` and 
`f8-local-deploy`.
