Trying out FAB in Apache Karaf
==============================

First you'll need a vanilla 2.3.x Karaf then install Fabric into it along with Camel for the shared-camel samples...

Install Fabric and Camel
------------------------

    features:addUrl mvn:io.fabric8/fabric8-karaf/1.1-SNAPSHOT/xml/features
    features:install fabric-bundle

To install Spring DM support

    features:install spring-dm

Install Camel for shared camel tests...

    features:addurl mvn:org.apache.camel.karaf/apache-camel/2.12.3/xml/features
    features:install camel-blueprint

Using a plain jar as a FAB
--------------------------

If you want to install a plain jar built using a maven-like tool so it includes a META-INF/services/maven/someGroup/someArtifact/pom.xml inside it with &lt;dependency&gt; for flat dependencies
and &lt;scope&gt;provided&lt;/scope&gt; for shared dependencies then just prefix the URL with **fab:**

The following example installs the [fab-sample-camel-blueprint-share sample jar](https://github.com/fabric8io/fabric8/tree/master/fab/tests/fab-sample-camel-blueprint-share)

    osgi:install fab:mvn:io.fabric8.fab.tests/fab-sample-camel-blueprint-share/1.1-SNAPSHOT

now start it and you're good to go!

If you're keen for more try [more examples](https://github.com/fabric8io/fabric8/tree/master/fab/tests)
