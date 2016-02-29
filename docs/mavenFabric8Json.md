## fabric8:json

The maven `fabric8:json` goal generates the `kubernetes.json` file for your [App](apps.html) from your Maven project and adds it as an artifact to your build so that it gets versioned and released along with your artifacts.

For a summary of the options see the [Maven Property Reference](#maven-properties)

### Generation options

You have various options for how to create the `kubernetes.json`

* hand craft yourself and put it into **src/main/resources** so that you can use Maven's default resource filtering to replace any project properties (e.g. the group ID, artifact ID, version number)
* let the fabric8:json goal generate it for you using its default rules and maven properties (see below)
* use the [annotation processors and typesafe builders](annotationProcessors.html) to create the metadata yourself; or enrich the default created metadata 
* you can enrich the generated JSON with additional metadata JSON file (using the `fabric8.extra.json` property which defaults to the file `target/classes/kubernetes-extra.json`)

If you have a maven project which is a typical microservice style application with a single [replication controller](replicationControllers.html) and [service](services.html) then we recommend just using the defaults that get generated; otherwise try the [annotation processors and typesafe builders](annotationProcessors.html) to create, edit or enrich the generated metadata (e.g. to add extra services).  

### Example usage

In a maven project type:

    mvn fabric8:json
    open target/classes/kubernetes.json

You'll then see the generated kubernetes JSON file.

### Creating OpenShift Templates

[OpenShift templates](http://docs.openshift.org/latest/dev_guide/templates.html) are an extension to Kubernetes JSON to allow parameters to be specified which are then specified by a user or generated as the template gets processed and applied.

To generate an OpenShift Template for your Maven project you just need to define one or more _parameters_ for your project using the maven properties `fabric8.parameter.FOO.description` and `fabric8.parameter.FOO.value`. Refer to the [Maven Property Reference](#maven-properties) for more details. 

### Specifying environment variables

You can use maven properties to specify environment variables to pass into the generated PodSpec in the ReplicationController as follows...

```
<project>
...
  <properties>
    <fabric8.env.FOO>bar</fabric8.env.FOO>
    ...
  </properties>
...
</project>
```

The above will then be the equivalent in docker terms of running...
```
docker run -d -e FOO=bar ${DOCKER_IMAGE} 
```

#### Templates and parameters

When using [OpenShift templates](http://docs.openshift.org/latest/dev_guide/templates.html) you want to parameterize things so users can more easily configure things. This means using expressions like `${FOO}` inside environment variable names or values.

One issue with this is that maven properties tend to expand expressions of the form `${FOO}` if there is a maven property or environment variable of that name. There is currently no way to escape those expressions inside maven property elements in the pom.xml `<properties/>` element.

So to make it easier to configure environment variables while bypassing maven's property expansion, you can use a file specified via `fabric8.envProperties` property which defaults to the file `src/main/fabric8/env.properties`. 
 
i.e. if you create a file called `src/main/fabric8/env.properties` in your project that looks like this

```
FOO = bar
ANOTHER = some ${CHEESE}
```
In the above example there will be 2 environment variables defined, `FOO` and `ANOTHER` with `ANOTHER` using a template parameter expression for `CHEESE`

Note that you can mix and match both approaches. The nice thing about maven properties is they can be inherited from parent projects. The nice thing about the `env.properties` file approach is you have more fine grained control over property expansion and dealing better with OpenShift template parameter expressions.

### Combining JSON files

The `fabric8:json` goal generates a kubernetes.json for each maven project which is enabled. Often maven projects are multi-module which means you'll have lots of fine grained generated `kubernetes.json` files. This is useful; but you often want to combine files together to make courser grained JSON files that are easier for users to consume. 

Another advantage of combining the JSON files together is that the `fabric8:json` goal automatically moves `Service` objects first; so that if you have cyclical apps which depend on each other, the combined JSON will force the services to be created up front before any Pods to avoid breaking links. (Services must be defined first so that their environment variables become available if using those for service discovery).

By default a `List` of items is created; unless the pom.xml defines any [OpenShift template](http://docs.openshift.org/latest/dev_guide/templates.html) parameters (see [Creating OpenShift Templates](##creating-openshift-templates) for more detail) or any of the dependent JSON files are `Template`. The `fabric8:json` goal automatically combines OpenShift Templates together; unifying the list of template parameters to create a single combined `Template`.

You can generate a separate JSON file with the dependencies of the current project, use <code>fabric8.combineJson.target</code> property for that. If you want to create a Template of the current project and its dependencies, you can set `fabric8.extra.json` property to `${fabric8.combineJson.target}`, and don’t forget to change the name of the Template (because "Combining JSON files" feature uses the names of templates for filtering duplicate), for example: `<fabric8.combineJson.project>${project.artifactId}Combine</fabric8.combineJson.project>`
 
#### Examples

The fabric8 project defines a number of different [application modules](https://github.com/fabric8io/quickstarts/tree/master/app-groups) for all the various parts of fabric8.

If you enable the `fabric8.combineDependencies` property then the `fabric8:json` goal will scan the maven dependencies for any dependency of `<classifier>kubernetes</classifier>` and `<type>json</type>` and combine them into the resulting JSON. 

See [this example](https://github.com/fabric8io/quickstarts/blob/master/app-groups/base/pom.xml#L35) to see how we can configure this in a pom.xml.

### Maven Properties

You can use maven properties to customize the generation of the JSON:

You define the maven properties in the `pom.xml` file using the `<properties>` tag such as:

```
    <properties>
      <fabric8.label.container>java</fabric8.label.container>
      <fabric8.label.group>myapp</fabric8.label.group>
      <fabric8.iconRef>camel</fabric8.iconRef>
    </properties>
```

If you wish to override or add a property from the command line, you can do this by using Java JVM system properties. A property from the command line will override any existing option configured in the `pom.xml` file. For example to add a 3rd label and change the icon, you can do:

    mvn fabric8:json -Dfabric8.label.foo=bar -Dfabric8.iconRef=java
  
There are many options as listed in the following table:
  
<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>docker.image</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the output docker image name.</td>
</tr>
<tr>
<td>fabric8.json.target</td>
<td>The generated kubernetes JSON file. Defaults to using the file <code>target/classes/kubernetes.json</code></td>
</tr>
<tr>
<td>fabric8.pureKubernetes</td>
<td>Should we exclude OpenShift templates and any extensions like OAuthConfigs in the generated or combined JSON? This defaults to <code>false</code></td>
</tr>
<tr>
<td>fabric8.combineDependencies</td>
<td>If enabled then the maven dependencies will be scanned for any dependency of <code>&lt;classifier&gt;kubernetes&lt;/classifier&gt;</code> and <code>&lt;type&gt;json&lt;/type&gt;</code> which are then combined into the resulting generated JSON file. See <a href="#combining-json-files">Combining JSON files</a></td>
</tr>
<tr>
<td>fabric8.combineJson.target</td>
<td>The generated kubernetes JSON file dependencies on the classpath. See <a href="#combining-json-files">Combining JSON files</a>. Defaults to using the property <code>fabric8.json.target</code></td>
</tr>
<tr>
<td>fabric8.combineJson.project</td>
<td>The project label used in the generated Kubernetes JSON dependencies template. See <a href="#combining-json-files">Combining JSON files</a>. This defaults to <code>${project.artifactId}</code></td>
</tr>
<tr>
<td>fabric8.container.name</td>
<td>The docker container name of the application in the generated JSON. This defaults to <code>${project.artifactId}-container</code></td>
</tr>
<tr>
<td>fabric8.containerPrivileged</td>
<td>Whether the generated container should be run in priviledged mode (defaults to false)</td>
</tr>
<tr>
<td>fabric8.envProperties</td>
<td>The properties file used to specify environment variables which allows ${FOO_BAR} expressions to be used without any Maven property expansion. Defaults to using the file <code>src/main/fabric8/env.properties</code></td>
</tr>
<tr>
<td>fabric8.env.FOO = BAR</td>
<td>Defines the environment variable FOO and value BAR.</td>
</tr>
<tr>
<td>fabric8.extra.json</td>
<td>Allows an extra JSON file to be merged into the generated kubernetes json file. Defaults to using the file <code>target/classes/kubernetes-extra.json</code>.</td>
</tr>
<tr>
<td>fabric8.extended.environment.metadata</td>
<td>Whether to try to fetch extended environment metadata during the json, or apply goals. The following ENV variables is supported: <tt>BUILD_URI</tt>, <tt>GIT_URL</tt>, <tt>GIT_COMMIT</tt>, <tt>GIT_BRANCH</tt>
    If any of these ENV variable is empty then if this option is enabled, then the value is attempted to be fetched from an online connection to the Kubernetes master. If the connection fails then the goal will report this as a failure gently and continue.
    This option can be turned off, to avoid any live connection to the Kubernetes master.
</td>
</tr>
<tr>
<td>fabric8.generateJson</td>
<td>If set to false then the generation of the JSON is disabled.</td>
</tr>
<tr>
<td>fabric8.iconRef</td>
<td>Provides the resource name of the icon to use; found using the current classpath (including the ones shipped inside the maven plugin). For example <code>icons/myicon.svg</code> to find the icon in the <code>src/main/resources/icons</code> directorty. You can refer to a common set of icons by setting this option to a value of: activemq, camel, java, jetty, karaf, mule, spring-boot, tomcat, tomee, weld, wildfly</td>
</tr>
<tr>
<td>fabric8.iconUrl</td>
<td>The URL to use to link to the icon in the generated Template.</td>
</tr>
<tr>
<td>fabric8.iconUrlPrefix</td>
<td>The URL prefix added to the relative path of the icon file</td>
</tr>
<tr>
<td>fabric8.iconBranch</td>
<td>The SCM branch used when creating a URL to the icon file. The default value is <code>master</code>.</td>
</tr>
<tr>
<tr>
<td>fabric8.imagePullPolicy</td>
<td>Specifies the image pull policy; one of <code>Always</code>,  <code>Never</code> or <code>IfNotPresent</code>, . Defaults to <code>Always</code> if the project version ends with <code>SNAPSHOT</code> otherwise it is left blank. On newer OpenShift / Kubernetes versions a blank value implies <code>IfNotPresent</code></td>
</tr>
<tr>
<td>fabric8.imagePullPolicySnapshot</td>
<td>Specifies the image pull policy used by default for <code>SNAPSHOT</code> maven versions.</td>
</tr>
<tr>
<td>fabric8.includeAllEnvironmentVariables</td>
<td>Should the environment variable JSON Schema files, generate by the **fabric-apt** API plugin be discovered and included in the generated kuberentes JSON file. Defaults to true.</td>
</tr>
<tr>
<td>fabric8.includeNamespaceEnvVar</td>
<td>Whether we should include the namespace in the containers' env vars. Defaults to <code>true</code.</td>
</tr>
<tr>
<td>fabric8.label.FOO = BAR</td>
<td>Defines the kubernetes label FOO and value BAR.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.initialDelaySeconds</td>
<td>Configures an initial delay in seconds before the probe is started.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.timeoutSeconds</td>
<td>Configures a timeout in seconds which the probe will use and is expected to complete within to be succesful.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.exec</td>
<td>Creates a exec action liveness probe with this command.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.httpGet.host</td>
<td>Creates a HTTP GET action liveness probe on this host. To use liveness probe with HTTP you must configure at least the host and path options.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.httpGet.port</td>
<td>Creates a HTTP GET action liveness probe on this port.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.httpGet.path</td>
<td>Creates a HTTP GET action liveness probe on with this path.</td>
</tr>
<tr>
<td>fabric8.livenessProbe.port</td>
<td>Creates a TCP socket action liveness probe on specified port.</td>
</tr>
<tr>
<td>fabric8.metrics.scrape</td>
<td>Enable/disable the export of metrics to Prometheus. See more details at <a href="http://fabric8.io/guide/metrics.html">metrics</a></td>
</tr>
<tr>
<td>fabric8.metrics.port</td>
<td>the request port to find metrics to export to Prometheus.</td>
</tr>
<tr>
<td>fabric8.metrics.scheme</td>
<td>the request scheme to find metrics to export to Prometheus.</td>
</tr>
<tr>
<td>fabric8.namespaceEnvVar</td>
<td>The name of the env var to add that will contain the namespace at container runtime. Defaults to <code>KUBERNETES_NAMESPACE</code>.</td>
</tr>
<tr>
<td>fabric8.parameter.FOO.description</td>
<td>Defines the description of the <a href="http://docs.openshift.org/latest/dev_guide/templates.html">OpenShift template</a> parameter <code>FOO</code>.</td>
</tr>
<tr>
<td>fabric8.parameter.FOO.value</td>
<td>Defines the value of the <a href="http://docs.openshift.org/latest/dev_guide/templates.html">OpenShift template</a> parameter <code>FOO</code>.</td>
</tr>
<tr>
<td>fabric8.port.container.FOO = 1234</td>
<td>Declares that the pod's container has a port named FOO with a container port 1234.</td>
</tr>
<tr>
<td>fabric8.port.host.FOO = 4567</td>
<td>Declares that the pod's container has a port port named FOO which is mapped to host port 4567.</td>
</tr>
<tr>
<td>fabric8.provider</td>
<td>The provider name to include in resource labels (defaults to <code>fabric8</code>).</td>
</tr>
<tr>
<td>fabric8.readinessProbe.initialDelaySeconds</td>
<td>Configures an initial delay in seconds before the probe is started.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.timeoutSeconds</td>
<td>Configures a timeout in seconds which the probe will use and is expected to complete within to be succesful.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.exec</td>
<td>Creates a exec action readiness probe with this command.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.httpGet.host</td>
<td>Creates a HTTP GET action readiness probe on this host. To use readiness probe with HTTP you must configure at least the host and path options.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.httpGet.port</td>
<td>Creates a HTTP GET action readiness probe on this port. The default value is 80.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.httpGet.path</td>
<td>Creates a HTTP GET action readiness probe on with this path.</td>
</tr>
<tr>
<td>fabric8.readinessProbe.port</td>
<td>Creates a TCP socket action readiness probe on specified port.</td>
</tr>
<tr>
<td>fabric8.replicas</td>
<td>The number of pods to create for the <a href="http://fabric8.io/guide/replicationControllers.html">Replication Controller</a> if the plugin is generating the App JSON file.</td>
</tr>
<tr>
<td>fabric8.replicationController.name</td>
<td>The name of the replication controller used in the generated JSON. This defaults to <code>${project.artifactId}-controller</code></td>
</tr>
<tr>
<td>fabric8.serviceAccount</td>
<td>The name of the service account to use in this pod (defaults to none)</td>
</tr>
<tr>
<td>fabric8.service.name</td>
<td>The name of the Service to generate. Defaults to <code>${project.artifactId}</code> (the artifact Id of the project)</td>
</tr>
<td>fabric8.service.headless</td>
<td>Whether or not we should generate headless services (services with no ports exposed, no cluster IPs, and are not managed my the Kube Proxy)</td>
</tr>
<tr>
<tr>
<td>fabric8.service.port</td>
<td>The port of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.type</td>
<td>The <a href="http://releases.k8s.io/HEAD/docs/user-guide/services.md#external-services">type of the service</a>. Set to <code>"LoadBalancer"</code> if you wish an
  <a href="https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/user-guide/services.md#type-loadbalancer"></a>external load balancer</a> to be created.</td>
</tr>
<tr>
<td>fabric8.service.containerPort</td>
<td>The container port of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.protocol</td>
<td>The protocol of the service. (If not specified then kubernetes will default it to TCP).</td>
</tr>
<tr>
<td>fabric8.service.port.&lt;portName&gt;</td>
<td>The service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.containerPort.&lt;portName&gt;</td>
<td>The container port to target to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.nodePort.&lt;portName&gt;</td>
<td>The node port of this service to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.protocol.&lt;portName&gt;</td>
<td>The protocol of this service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.port</td>
<td>The port of the Service to generate for service &lt;name&gt;.</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.type</td>
<td>The <a href="http://releases.k8s.io/HEAD/docs/user-guide/services.md#external-services">type of the service</a>. Set to <code>"LoadBalancer"</code> if you wish an
  <a href="https://github.com/GoogleCloudPlatform/kubernetes/blob/master/docs/user-guide/services.md#type-loadbalancer"></a>external load balancer</a> to be created.</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.containerPort</td>
<td>The container port of the Service to generate (if a kubernetes service is required).</td>
</tr>
<tr>
<td>fabric8.service.protocol</td>
<td>The protocol of the service. (If not specified then kubernetes will default it to TCP).</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.port.&lt;portName&gt;</td>
<td>The service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.containerPort.&lt;portName&gt;</td>
<td>The container port to target to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.nodePort.&lt;portName&gt;</td>
<td>The node port of this service to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.service.&lt;name&gt;.protocol.&lt;portName&gt;</td>
<td>The protocol of this service port to generate (if a kubernetes service is required with multiple ports).</td>
</tr>
<tr>
<td>fabric8.volume.FOO.emptyDir = somemedium</td>
<td>Defines the emtpy volume with name FOO and medium somemedium.</td>
</tr>
<tr>
<td>fabric8.volume.FOO.hostPath = /some/path</td>
<td>Defines the host dir volume with name FOO.</td>
</tr>
<tr>
<td>fabric8.volume.FOO.mountPath = /some/path</td>
<td>Defines the volume mount with name FOO.</td>
</tr>
<tr>
<td>fabric8.volume.FOO.readOnly</td>
<td>Specifies whether or not a volume is read only.</td>
</tr>
<tr>
<td>fabric8.volume.FOO.secret = BAR</td>
<td>Defines the secret name to be BAR for the FOO volume.</td>
</tr>
</table>

