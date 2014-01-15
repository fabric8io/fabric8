Fuse Fabric

This binary distribution contains the files needed to install Fabric on a Karaf instance.

To install fabric, copy the content of the system folder into the Karaf system folder.
Then run the following commands:
   > features:addurl mvn:io.fabric8/fabric8-karaf/${project.version}/xml/features
   > features:install fabric-command

Enjoy!
