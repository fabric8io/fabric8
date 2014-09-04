## Property Resolving

When using properties files in Fabric8 for implementing a distributed OSGi Config Admin; we support a number of different property resolvers.

So there is normal variable expansion for system properties. e.g. 

```
myDir = ${runtime.data}/cheese
```

would default to the data directory of the current container plus "/cheese". However there are a number of additional property resolvers available using the ${...:...} format.

### Container

When you wish to refer to the current containers network settings; such as its ip, local host name or bind address:

```
localhostname = ${container:localhostname}
ip = ${container:ip}
bindaddress = ${container:bindaddress}
```

### Checksum

When you wish to calculate the checksum of a file or URL:
```
mychecksum = ${checksum:profile:\foo.xml}
```

### Crypt

When you wish to decrypt an encrypted value (such as a password):
```
mypassword = ${crypt:ABCDEF}
```

### Env

For accessing an environment variable.

```
bindPort=${env:OPENSHIFT_FUSE_AMQ_PORT}
```

You can use a the elvis operator a little like with [groovy](http://docs.groovy-lang.org/docs/next/html/documentation/core-operators.html#_elvis_operator) to specify a default value if an environment variable is not set

```
bindPort=${env:OPENSHIFT_FUSE_AMQ_PORT?:1234}
```

Where the text after the **?:** token is used as the default value if the environment variable is not set

### Groovy

This property resolver allows you to use [Groovy](http://groovy-lang.org/) script expressions to dynamically resolve values; particularly using the API calls on the [ZooKeeperFacade](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-zookeeper/src/main/java/io/fabric8/zookeeper/utils/ZooKeeperFacade.java#L30) interface.

e.g. to dynamically lookup the available Cassandra hosts you can use this expression

```
bindPort=${groovy:zk.matchingDescendantStringData("/fabric/registry/clusters/cassandra/default/*/listen").join(",")}
```

Where the **matchingDescendantStringData()** function on the **zk** object looks up all the listen hosts in the ZooKeeper registry (using '*' to indicate a wildcard path) and then joins the values with a comma.

You can also do things like [use the elvis operator](http://docs.groovy-lang.org/docs/next/html/documentation/core-operators.html#_elvis_operator) to use an environment variable or system property value or use a default value if not using expressions such as

```
something=${groovy:env.MY_ENV_VAR ?: 'someDefaultValue'}
something=${groovy:sys['foo.bar'] ?: 'anotherThing'}
```

#### Helper objects

<table class="table table-striped">
<tr>
<th>Environment Variable</th>
<th>Description</th>
</tr>
<tr>
<td>env</td>
<td>A Map of all the environment variables available</td>
</tr>
<tr>
<td>sys</td>
<td>A Map of all the system properties available</td>
</tr>
<tr>
<td>zk</td>
<td>An instance of the <a href="https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-zookeeper/src/main/java/io/fabric8/zookeeper/utils/ZooKeeperFacade.java#L30">ZooKeeperFacade class</> for making it easy to query and navigate around ZooKeeper and extract values</td>
</tr>
</table>

### Port

When running multiple child containers on a machine, you need to associate ports to JVMs. Fabric8 supports port allocation using a property resolver of the form...

```
fooPort = ${port:1234-4567}
```

Where the 2 numbers represent the port range to use to pick a port. The expression will then evaluate to the port allocated to the current JVM.

### Profile

This allows you to refer to a value in a config admin PID in a profile. e.g.

```
foo = ${profile:myPid/myKey}
```

would resolve to the config admin PID of "myPid" and key "myKey". e.g. if the current container's profile was "foo" it would look in /fabric/profiles/foo/myPid.properties and find the **myKey** value.

### Version

This resolves to a version number for a given key. Its often use to refer to a specific bundle or feature

```
bundle.mything = mvn:groupId/artifact/${version:camel}
```

What happens is the resolver looks up in the current profile the property file called **io.fabric8.version.properties** and then looks for the key "camel" (in this case) and uses that for the version value.

This means that there's a single place to define all the versions of things; which can be changed on a per profile (or version) basis easily - to avoid littering your profiles with version numbers and making it really easy to do a patch upgrade of versions of things.

### ZooKeeper

This resolver evaluates the ZooKeeper path:

```
identity=${zk:/fabric/registry/cloud/config/aws-ec2/identity}
credential=${zk:/fabric/registry/cloud/config/aws-ec2/credential}
```
