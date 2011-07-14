Trying out FAB in Karaf
=======================

To try out FAB type the following into a Karaf shell in a vanilla 2.2.x Karaf...

Install Fabric and Camel
------------------------

    features:addUrl mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features
    features:install fabric-fab

Install Camel for shared camel tests...

    features:addurl mvn:org.apache.camel.karaf/apache-camel/2.7.1-fuse-00-39/xml/features
    features:install camel-blueprint

You'll need to make sure you have the Fuse repo on your "org.ops4j.pax.url.mvn.repositories" property to be able to install the Fuse camel feature...

* http://repo.fusesource.com/nexus/content/groups/public

Using a plain jar as a FAB
--------------------------

If you want to install a plain jar built using a maven-like tool so it includes a META-INF/services/maven/someGroup/someArtifact/pom.xml inside it with <dependency> for flat dependencies
and <scope>provided</scope> for shared dependencies then just prefix the URL with **fab:**

The following example installs the [fab-sample-camel-blueprint-share sample jar|https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-blueprint-share]

    osgi:install fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-blueprint-share/1.0-SNAPSHOT

now start it and you're good to go!