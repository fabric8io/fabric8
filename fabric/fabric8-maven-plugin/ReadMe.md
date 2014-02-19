Fabric8 Maven Plugin
====================

This maven plugin makes it easy to lazily create a profile in a fabric and add/update it to use the artifact from a maven build.

To use it from a maven project run:

    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

The project will then be built and deployed into a profile in the fabric. By default the profile is named $group-$artifact but you can override the profile ID, version ID and location of the Fabric Jolokia URL via the maven plugin configuration.

