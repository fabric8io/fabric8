## Web Console

The Web Console is based on the [hawtio project](http://hawt.io/) and provides a rich HTML5 based web application for working with Kubernetes and the underlying containers reusing the various [hawtio plugins](http://hawt.io/plugins/index.html).

### Using Jube

If you are using Jube then the web console should be visible at [http://localhost:8585/hawtio/](http://localhost:8585/hawtio/). You can then view these tabs:

 * [Pods tab](http://localhost:8585/hawtio/kubernetes/pods) views all the available [pods](pods.html) in your kubernetes environment
 * [Replication Controllers tab](http://localhost:8585/hawtio/kubernetes/replicationControllers) views all the available [replication controllers](replicationControllers.html) in your kubernetes environment
 * [Services tab](http://localhost:8585/hawtio/kubernetes/services) views all the available [services](services.html) in your kubernetes environment

### Using Kubernetes/OpenShift

If you are using Kubernetes or OpenShift you need to find the URL that the web console is running. From there you should be able to navigate to the tabs for [pods](pods.html), [replication controllers] and [services](services.html)

