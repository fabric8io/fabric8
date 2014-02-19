Fabric8 Maven Plugin
====================

This maven plugin makes it easy to lazily create a profile in a fabric and add/update it to use the artifact from a maven build.

To use it from a maven project run:

    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

The project will then be built and deployed into a profile in the fabric. By default the profile is named $group-$artifact but you can override the profile ID, version ID and location of the Fabric Jolokia URL via the maven plugin configuration.

e.g. to try it out

    cd quickstarts/rest
    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

Then you should see this profile being created: http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/org.jboss.quickstarts.fuse/rest.profile
Which should have a bundle added too.