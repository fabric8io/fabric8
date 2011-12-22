Karaf integration with LibVirt.

Installtion
-----------
1) Add jna.library.path to etc/system.properties pointing to the installtion location of libvirt.
2  Modify libvirtd.conf to ensure that the user that runs Karaf has write permissions to libvrit socket.

e.g. set these values in your ~/.libvirt/libvirtd.conf

  unix_sock_ro_perms = "0777"
  unix_sock_rw_perms = "0777"

3) Start the libvirtd
4) Start Karaf (make sure the user that runs Karaf has the required permissions to use libvirt).
5) Add the feature:
   karaf@root>features:addurl mvn:org.fusesource.fabric/fuse-fabric/1.1-SNAPSHOT/xml/features
   karaf@root>features:install virt
6) Create a new virt service using the virt managed service factory:
   karaf@root>conf:edit org.fusesource.fabric.virt-<name>
   karaf@root>conf:propset url vbox:///session (this is for virtualbox).
   karaf@root>conf:update
7) Use the shell to list, start or stop domains:
   karaf@root>virt:domain-list

   [Id.] [Name]               [State]
       1 Ubuntu               VIR_DOMAIN_SHUTOFF

   karaf@root>virt:domain-start Ubuntu (in this case Ubuntu is the name of the domain).
   karaf@root>virt:domain-stop Ubuntu (in this case Ubuntu is the name of the domain).

Enjoy!
