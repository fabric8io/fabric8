## Property Resolving

When using properties files in Fabric8 for implementing a distributed OSGi Config Admin; we support a number of different property resolvers.

So there is normal variable expansion for system properties. e.g. 

```
myDir = ${karaf.data}/cheese
```

would default to the data directory of the current container plus "/cheese". However there are a number of additional property resolvers available using the ${...:...} format.

### Container

When you wish to refer to the current containers network settings; such as its ip, local host name or bind address:

```
localhostname = ${container:localhostname}
ip = ${container:ip}
bindaddress = ${container:bindaddress}
```

### Env

For accessing an environment variable.

```
bindPort=${env:OPENSHIFT_FUSE_AMQ_PORT}
```


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

