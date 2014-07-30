## Integration Tests Using Remote Container

This maven project runs the integration tests against a remote container; using the fabric8-karaf distribution by default and using the **bin/fabric8-start** shell script to create the initial root node and fabric.

### ArchetypeTest

This integration test iterates through all the archetypes generated from the [quickstarts](https://github.com/fabric8io/fabric8/tree/master/quickstarts) and

* generates a new project from the archetype and builds it
* performs a [mvn fabric8:deploy](http://fabric8.io/gitbook/mavenPlugin.html#using-the-plugin) on the fabric to create a new profile
* asserts using the [auto scaler](http://fabric8.io/gitbook/requirements.html) that the test can provision a working container from the profile

By default each archetype is tested in turn; with failures being reported at the end (so that you know all the failed archetypes).

#### Running ArchetypeTest with a single archetype

If you want to run ArchetypeTest with a single archetype id, just pass the **ArchetypeTest.artifactId** system property in on the maven command line. e.g.

    mvn clean install -Pts.archetype -DArchetypeTest.artifactId=mythingy

Also there's a handy **archetypeTest.sh** which does the above for all archetypes or for the given artifact ID pattern; piping all output to the mvn.log file so you can see the entire history of what happened if you run it locally.

e.g. to run all the java container archetype tests:

    ./archetypeTest.sh java

or the camel related tests:

    ./archetypeTest.sh camel
