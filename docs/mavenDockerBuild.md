## docker:build

The maven `fabric8:build` goal pushes the docker image generated via [mvn docker:build](mavenDockerBuild.html) to either the global docker registry or a local docker registry.

To create a container on Kubernetes you need to create a docker image. To enable the creation of a docker image use a maven plugin such as the [docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin/blob/master/README.md)

Then to build the docker image locally its

    mvn install docker:build

### Reference

See the [reference docs for docker:build](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#dockerbuild)