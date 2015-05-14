## fabric8:apply

The maven `fabric8:apply` goal takes the JSON file generated via [mvn fabric8:json](mavenFarbic8Json.html) located at `target/classes/kubernetes.json` and applies it to the current Kubernetes environment and namespace. 

### Prerequisites

Note that before trying to apply your Kubernetes JSON you need to make sure your docker image is available to Kubernetes. See the [mvn docker:build](mavenDockerBuild.html) and [mvn docker:push](mavenDockerPush.html) goals. 

Also if you are using a local docker registry, make sure it is network accessible to your Kubernetes cluster.

## Example

To apply your App use the following goal:

    mvn fabric8:apply

### Property Reference for fabric8:apply

The following maven property values are used to configure the behaviour of the apply goal:

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
<td>docker.registry</td>
<td>Used by the <a href="https://github.com/rhuss/docker-maven-plugin/blob/master/README.md">docker-maven-plugin</a> to define the local docker registry to use (if not using the public docker registry).</td>
</tr>
</table>

