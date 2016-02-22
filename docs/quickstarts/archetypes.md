### Archetypes

For each quickstart a dedicate archetype is available. You can create
a new project based on the archetype easily:

    mvn archetype:generate \
          -DarchetypeGroupId=io.fabric8.archetypes \
          -DarchetypeArtifactId=cdi-camel-archetype \
          -DarchetypeVersion=2.2.96

You will be asked for some project parameters like the `groupId` or
the `artifactId`:

```
[INFO] --- maven-archetype-plugin:2.3:generate (default-cli) @ standalone-pom ---
[INFO] Generating project in Interactive mode
[INFO] Archetype repository not defined. Using the one from [io.fabric8.archetypes:cdi-camel-archetype:2.2.96] found in catalog remote
Define value for property 'groupId': : myf8
Define value for property 'artifactId': : cdi-demo
Define value for property 'version':  1.0-SNAPSHOT: :
Define value for property 'package':  myf8: :
Confirm properties configuration:
groupId: myf8
artifactId: cdi-demo
version: 1.0-SNAPSHOT
package: myf8
 Y: :
```

The only two required properties are `groupId` and `artifactId` so you
can create the project also easily with batch mode, providing the
parameters as Maven properties:


    mvn archetype:generate -B \
          -DarchetypeGroupId=io.fabric8.archetypes \
          -DarchetypeArtifactId=cdi-camel-archetype \
          -DgroupId=myf8 \
          -DartifactId=d2 \
          -DarchetypeVersion=2.2.96

The name of the `archetypeArtifactId` consists of the category
(`cdi`,`java`,`karaf`,`spring-boot` or `war`), the subproject
within it (e.g. `camel` for `cdi` but see also the
[overview](index.md)) and the fixed suffix `-archetype`.

