Trying out FAB in Karaf
=======================

To try out FAB type the following into a Karaf shell in a vanilla 2.2.x Karaf...

    features:addUrl mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features
    features:install fabric-fab

Install Camel for shared camel tests...

    features:addurl mvn:org.apache.camel.karaf/apache-camel/2.7.1-fuse-00-39/xml/features
    features:install camel-blueprint

Then to try install something - for example the plain jar "fab-sample-camel-blueprint-share"...

    osgi:install fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-blueprint-share/1.0-SNAPSHOT

now start it!

You'll need to make sure you have the Fuse repo on your "org.ops4j.pax.url.mvn.repositories" property...

* http://repo.fusesource.com/nexus/content/groups/public
