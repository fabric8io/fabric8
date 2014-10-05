## Builds

Builds are a [kubernetes extension in OpenShift 3](https://github.com/openshift/origin/blob/master/docs/builds.md) which support various mechanisms for building containers via

* supplying pre-built deployment units
* having source code in a git repository where a git push kicks off a build of the project artefacts along with creating a docker container