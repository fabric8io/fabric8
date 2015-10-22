## fabric8:clean

The maven `fabric8:clean` goal deletes all resources in the current Kubernetes namespace.


| Option                  | Type    | Description                                  |
|----------------------------------------------------------------------------------|
| **fabric8.deep.clean**  | Boolean | Flag to enable deep clean (see below)        |
| **fabric8.namespace**   | String  | The namespace to clean (defaults to current) |


Affected kubernetes resources are:

- [services](services.html)
- [replication controllers](replicationControllers.html)
- [pods](pods.html)
- endpoints
- events

Affected Openshift resources are:

- routes
- builds
- build configs
- deployment configs
- image streams
- templates

## Deep clean up

The command accepts an extra option that allows deep cleaning. In this case, the following resources will also get deleted.

- secrets
- service accounts
- security context constraints