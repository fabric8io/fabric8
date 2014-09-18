## Environment variables

Fabric8 supports a number of environment variables which make it easy to configure how a container starts up. The various options are

 * create a new local fabric on startup (the default)
 * join an existing fabric
 * startup in stand alone mode (no fabric)

The use of environment variables is particularly useful for when you are using Fabric8 with some kind of cloud technologies like [Docker](docker.html), [OpenShift](https://www.openshift.com/quickstarts/jboss-fuse-61), [OpenStack](cloudContainers.html), [EC2](cloudContainers.html) etc.

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
<tr>
<td>FABRIC8_PROFILES</td>
<td>The initial profiles to add to a newly created fabric.</td>
</tr>
<tr>
<td>FABRIC8_GLOBAL_RESOLVER</td>
<td>Whether to use manualip, localip or localhostname to resolve names.</td>
</tr>
<tr>
<td>FABRIC8_MANUALIP</td>
<td>What IP address should be used if the 'manualip' resolver is used (see above).</td>
</tr>
<tr>
<td>FABRIC8_BINDADDRESS</td>
<td>What network address should the container bind to.</td>
</tr>
<tr>
<td>FABRIC8_RUNTIME_ID</td>
<td>The default container name to use.</td>
</tr>
<tr>
<td>FABRIC8_KARAF_NAME</td>
<td>The default container name to use (now <b>deprecated</b>, please use FABRIC8_RUNTIME_ID instead).</td>
</tr>
</table>
