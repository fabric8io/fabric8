Camel Router WAR OSGi Project
=============================

This project includes a sample route as as a WAR.
You can build the WAR by running

    mvn install

You can then run the project by dropping the WAR into your 
favorite web container or just run

    mvn jetty:run

to start up and deploy to Jetty.

This project also adds the necessary metadata to the WAR
to allow deploying it in JBoss Fuse with:

    osgi:install -s war:mvn:${groupId}/${artifactId}/${version}/war

For more help see the Apache Camel documentation

    http://camel.apache.org/

