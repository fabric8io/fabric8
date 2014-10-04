## URL handlers

Fabric8 supports a number of different URL handlers to make it easy to work with things like maven repositories, ZooKeeper etc.

### blueprint and spring

These URL handlers boot up the remaining URL as a blueprint or spring XML file respectively. 

```
blueprint:http://foo.com/myblueprint.xml
spring:file:///mydirectory/myspring.xml
```

The remainder after the "blueprint:" or "spring:" prefix is interpreted as another URL so this URL can be combined with other URL schemes like file, http, mvn, profile, zk etc

### mvn

The **mvn:** URL handler allows you to access files and resources from maven repositories using the familar maven 'coordinates' of group, artifact, version with optional type and classifier.

e.g. to refer to an apache camel jar

```
mvn:org.apache.camel/camel-core/2.12.0
```

### profile

The **profile:** URL handler makes it easy to refer to a profile's configuration file which exists in one of the current profiles of the current container; or in a parent profile.

e.g.

```
profile:camel.xml
```
will refer to the file called "camel.xml" either in /fabric/profiles/foo for the current profile (foo) or in a parent profiles directory.

You can then startup blueprint or spring XML files as a bundle using this URL with the blueprint or spring URL handlers as follows inside a profile configuration:

```
bundle.foo = blueprint:profile:foo.xml
bundle.bar = spring:profile:bar.xml
```

### mvel

The **mvel:** URL handler allows you to render templates based on the effective profile or runtime properties. The profile object can be accessed using the "profile" variable
and the runtime properties using the "runtime" one.

e.g.

```
mvel:profile:jetty.xml
```
will refer to the template file called "jetty.xml" either in the current profile or in a parent profiles directory.

If you need more information about the mvel language, please visit http://mvel.codehaus.org/

As for the profile URL handler, you can then startup blueprint or spring XML files as a bundle using this URL with the blueprint or spring URL handlers

```
bundle.foo = blueprint:mvel:profile:foo.xml
bundle.bar = spring:mvel:profile:bar.xml
```

### zk

The **zk:** URL handler lets you refer to a path in ZooKeeper like you can with file: or http:.

```
zk:/my/path/here
```


