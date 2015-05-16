## fabric8:create-routes

The maven `fabric8:create-routes` goal queries all the [Services](services.html) in the current namespace which expose ports 80 or 443 and create a new [OpenShift Route](http://docs.openshift.org/latest/admin_guide/router.html) if one doesn't already exist

You can use the maven property `fabric8.domain` or the environment variable `$KUBERNETES_DOMAIN` to define the DNS host to expose services as [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html)

For a summary of the options see the [Maven Property Reference](#maven-properties)

### Default Configuration

The Kubernetes environment and authentication is defined by the [kubernetes-api environment variables](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api#configuration). In particular:

* `KUBERNETES_SERVICE_HOST`:`KUBERNETES_SERVICE_PORT` / `KUBERNETES_MASTER` - the location of the kubernetes master
* `KUBERNETES_NAMESPACE` - the default namespace used on operations

#### Defaults from OpenShift

If no configuration is supplied through maven properties or environment variables, the `fabric8:create-routes` goal will try to find the current login token and namespace by parsing the users `~/.config/openshift/config` file.

This means that if you use the [OpenShift](http://www.openshift.org/) command line tool `osc` you can login and change projects (namespaces in kubernetes speak) and those will be used by default by the `fabric8:create-routes` goal. e.g.

```
osc login
osc project cheese
mvn fabric8:create-routes
```
In the above, if there is no `KUBERNETES_NAMESPACE` environment variable or maven property called `fabric8.namespace` then the `fabric8:create-routes` goal will apply the Kubernetes resources to the `cheese` namespace.

## Example

To generate any missing routes for services use the following goal:

    mvn fabric8:create-routes

Note if you are not in a maven project which has the [fabric8 maven plugin enabled](mavenPlugin.html) then you can use the more verbose version:

    mvn io.fabric8:fabric8-maven-plugin:2.1.1:create-routes

To specify an explicit namespace and domain in recreate mode:

    mvn fabric8:create-routes -Dfabric8.domain=foo.acme.com -Dfabric8.namespace=cheese   

### Templates

Applying an [OpenShift template](http://docs.openshift.org/latest/dev_guide/templates.html) works the same as a regular `List` of Kubernetes resources. 

One difference is that you may wish to override some of the template parameter values as you apply the template which you can do on the command line via system properties.

    mvn fabric8:create-routes -Dfabric8.apply.FOO=bar
    
The above will apply the OpenShift template defined in `target/classes/kubernetes.json` overriding the template parameter `FOO` with the value `bar` before processing the template and creating/updating the resources.    

### Maven Properties

The following maven property values are used to configure the behaviour of the apply goal:

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>fabric8.apply.create</td>
<td>Should we create new resources (not in the kubernetes namespace). Defaults to <code>true</code>.</td>
</tr>
<tr>
<td>fabric8.apply.servicesOnly</td>
<td>Should only services be processed. This lets you run 2 builds, process the services only first; then process non-services. Defaults to <code>false</code>.</td>
</tr>
<tr>
<td>fabric8.apply.ignoreServices</td>
<td>Ignore any services in the JSON. This is useful if you wish to recreate all the ReplicationControllers and Pods but not recreate Services (which can cause <code>PortalIP</code> addresses to change for services which can break some Pods and could cause problems for load balancers. Defaults to <code>false</code>.</td>
</tr>
<tr>
<td>fabric8.apply.createRoutes</td>
<td>If there is a route domain (see <code>fabric8.domain</code>) then this option will create an <a href="http://docs.openshift.org/latest/admin_guide/router.html">OpenShift Route</a> for each service for the host expressio: <code>${serviceName}.${fabric8.domain}</code>. Defaults to <code>true</code>.</td>
</tr>
<tr>
<td>fabric8.domain</td>
<td>The domain to expose the services as <a href="http://docs.openshift.org/latest/admin_guide/router.html">OpenShift Routes</a>. Defaults to <code>$KUBERNETES_DOMAIN</code>.</td>
</tr>
<tr>
<td>fabric8.namespace</td>
<td>Specifies the namespace (or OpenShift project name) to apply the kubernetes resources to. If not specified it will use the <code>KUBERNETES_NAMESPACE</code> environment variable or use the <a href="#defaults-from-openshift">Defaults from OpenShift</a></td>
</tr>
<tr>
<td>fabric8.recreate</td>
<td>If enabled then all resources that are found are deleted first before being recreated. Defaults to <code>false</code>.</td>
</tr>
</table>

