## fabric8:create-env

The maven `fabric8:create-env` goal queries all the [Services](services.html) in the current namespace along with looking at the JSON file generated via [mvn fabric8:json](mavenFarbic8Json.html) (located at `target/classes/kubernetes.json`) and generates a list of environment variables that would be required to simulate running a process inside Kubernetes.

This makes it easy to run, say, Java programs in your IDE as if it was running inside a Kubernetes environment; discovering all the [services](services.html) available in the namespace and setting all the same environment variables that are defined inside your [pods](pods.html) or [replication controllers](replicationControllers.html)

For a summary of the options see the [Maven Property Reference](#maven-properties)

### Generated files

The goal generates  the following files:
 
 * `target/env.sh` you can `source` in a shell to setup your environment together 
 * `target/env.properties` file you can process easily with Java code or ideally your IDE can accept in its UI for running applications or test cases

### Acessing services 

Note that your local machine may not be able to see [services](services.html) using the `PortalIP` address of the service (depending on how your networking is setup).

So the service environment variables like `FOO_SERVICE_HOST` will use the host names on any defined [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) instead which allow access to services from outside of the Kubernetes cluster.

If you do not have [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) setup for your services then you can either:

* apply them via [fabric8:apply](mavenFabric8Apply.html) so that it [creates the routes](mavenFabric8Apply.html#creating-routes) by specifying the maven property `fabric8.domain` or the environment variable `$KUBERNETES_DOMAIN` to specify to the DNS/host domain you wish to expose the services on.
* use [fabric8:create-routes](mavenFabric8CreateRoutes.html) to generate any missing [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) for the current services using the maven property `fabric8.domain` or the environment variable `$KUBERNETES_DOMAIN` to specify to the DNS/host domain you wish to expose the services on.

### Default Configuration

The Kubernetes environment and authentication is defined by the [kubernetes-api environment variables](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api#configuration). In particular:

* `KUBERNETES_SERVICE_HOST`:`KUBERNETES_SERVICE_PORT` / `KUBERNETES_MASTER` - the location of the kubernetes master
* `KUBERNETES_NAMESPACE` - the default namespace used on operations

#### Defaults from OpenShift

If no configuration is supplied through maven properties or environment variables, the `fabric8:create-env` goal will try to find the current login token and namespace by parsing the users `~/.config/openshift/config` file.

This means that if you use the [OpenShift](http://www.openshift.org/) command line tool `oc` you can login and change projects (namespaces in kubernetes speak) and those will be used by default by the `fabric8:create-env` goal. e.g.

```
oc login
oc project cheese
mvn fabric8:create-env
```
In the above, if there is no `KUBERNETES_NAMESPACE` environment variable or maven property called `fabric8.namespace` then the `fabric8:create-env` goal will look at the Kubernetes resources to the `cheese` namespace.

## Example

To setup your shell as if its running inside a Kubernetes environment container:
 
    mvn fabric8:create-env
    source target/env.sh

To specify an explicit namespace:

    mvn fabric8:create-env -Dfabric8.namespace=cheese   

### Maven Properties

The following maven property values are used to configure the behaviour of the apply goal:

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>fabric8.namespace</td>
<td>Specifies the namespace (or OpenShift project name) to apply the kubernetes resources to. If not specified it will use the <code>KUBERNETES_NAMESPACE</code> environment variable or use the <a href="#defaults-from-openshift">Defaults from OpenShift</a></td>
</tr>
</table>

