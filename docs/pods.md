## Pods

[Pods](https://github.com/GoogleCloudPlatform/kubernetes/blob/master/DESIGN.md#pods) are a Kubernetes concept which maps to one or more docker containers running on the same host. Typically in a fabric8 context this maps to a single container. Though a pod abstraction allows you to co-locate multiple containers together on a single box. e.g. a httpd container and 2 tomcats; or a wildfly and a redis container etc.

