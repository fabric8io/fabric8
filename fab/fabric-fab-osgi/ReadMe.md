Trying out FAB in Karaf
=======================

To try out FAB type the following into a Karaf shell...

  features:addUrl mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features
  features:install fabric-fab


Then to try install something...

  osgi:install fab:mvn:org.fusesource.fabric.fab.tests/fab-sample-camel-noshare/1.0-SNAPSHOT