## Environment variables

Fabric8 supports a number of environment variables which make it easy to configure how a container starts up. The various options are

 * create a new local fabric on startup (the default)
 * join an existing fabric
 * startup in stand alone mode (no fabric)

The use of environment variables is particularly useful for when you are using Fabric8 with some kind of cloud technologies like [Docker](http://fabric8.io/#/site/book/doc/index.md?chapter=docker_md), [OpenShift](https://www.openshift.com/quickstarts/jboss-fuse-61), [OpenStack](http://fabric8.io/#/site/book/doc/index.md?chapter=cloudContainers_md), [EC2](http://fabric8.io/#/site/book/doc/index.md?chapter=cloudContainers_md) etc.

i.e. you can use the same distribution, [docker container image](https://github.com/fabric8io/fabric8-docker#fabric8-docker) or virtual machine image and change how you wish the container to behave just via environment variables.

### Variables reference

<table class="table table-striped">
<tr>
<th>Environment Variable</th>
<th>Description</th>
</tr>
<tr>
<td>FABRIC8_ENSEMBLE_AUTO_START</td>
<td>Value is 'true' or 'false' depending on if the container should auto-start an ensemble.</td>
</tr>
<tr>
<td>FABRIC8_AGENT_AUTO_START</td>
<td>Value is 'true' or 'false' depending on if the container should auto-start the fabric8 agent.</td>
</tr>
<tr>
<td>FABRIC8_ZOOKEEPER_URL</td>
<td>The URL of the ZooKeeper server(s) to connect to on startup; when joining a fabric on startup. e.g. hostname:2181</td>
</tr>
<tr>
<td>FABRIC8_ZOOKEEPER_PASSWORD</td>
<td>The password used when automatically creating a fabric on startup.</td>
</tr>
</table>