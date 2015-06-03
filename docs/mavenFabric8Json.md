## fabric8:json

The maven `fabric8:json` goal generates the `kubernetes.json` file for your [App](apps.html) from your Maven project and adds it as an artifact to your build so that it gets versioned and released along with your artifacts.

For a summary of the options see the [Maven Property Reference](#maven-properties)

### Generation options

You have various options for how to create the `kubernetes.json`

* hand craft yourself
* you can put it into **src/main/resources** so that you can use Maven's default resource filtering to replace any project properties (e.g. the group ID, artifact ID, version number)
* let the fabric8:json goal generate it for you using its default rules and maven properties (see below)
* you can enrich the generated JSON with additional metadata JSON file (using the `fabric8.extra.json` property which defaults to the file `target/classes/kubernetes-extra.json`)
* use the typesafe builders in the Java [kubernetes-api API](https://github.com/fabric8io/fabric8/tree/master/components/kubernetes-api) to create the metadata yourself 


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
 
#### Examples

The fabric8 project defines a number of different [application modules](https://github.com/fabric8io/quickstarts/tree/master/app-groups) for all the various parts of fabric8.

If you enable the `fabric8.combineDependencies` property then the `fabric8:json` goal will scan the maven dependencies for any dependency of `<classifier>kubernetes</classifier>` and `<type>json</type>` and combine them into the resulting JSON. 

See [this example](https://github.com/fabric8io/quickstarts/blob/master/app-groups/base/pom.xml#L35) to see how we can configure this in a pom.xml.

### Maven Properties

You can use maven properties to customize the generation of the JSON:

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
<td>fabric8.combineDependencies</td>
<td>If enabled then the maven dependencies will be scanned for any dependency of <code>&lt;classifier&gt;kubernetes&lt;/classifier&gt;</code> and <code>&lt;type&gt;json&lt;/type&gt;</code> which are then combined into the resulting generated JSON file. See <a href="#combining-json-files">Combining JSON files</a></td>
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
<td>fabric8.env.FOO = BAR</td>
<td>Defines the environment variable FOO and value BAR.</td>
</tr>
<tr>
<td>fabric8.extra.json</td>
<td>Allows an extra JSON file to be merged into the generated kubernetes json file. Defaults to using the file <code>target/classes/kubernetes-extra.json</code>.</td>
</tr>
<tr>
<td>fabric8.generateJson</td>
<td>If set to false then the generation of the JSON is disabled.</td>
</tr>
<tr>
<td>fabric8.imagePullPolicy</td>
<td>Specifies the image pull policy; one of <code>Always</code>,  <code>Never</code> or <code>IfNotPresent</code>, . Defaults to <code>Always</code> if the project version ends with <code>SNAPSHOT</code> otherwise it is left blank. On newer OpenShift / Kubernetes versions a blank value implies <code>IfNotPresent</code></td>
</tr>
<tr>
<td>fabric8.imagePullPolicySnapshot</td>
<td>Specifies the image pull policy used by default for <code>SNAPSHOT</code> maven versions. Defaults to <code>Always</code></td>
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
<tr>
<td>fabric8.service.port</td>
<td>The port of the Service to generate (if a kubernetes service is required).</td>
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
<td>fabric8.service.protocol.&lt;portName&gt;</td>
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

