Karaf integration with LibVirt.

Installtion
-----------
1) Add jna.library.path to etc/system.properties pointing to the installtion location of libvirt lib directory.

  This is probably in a directory like the following:
     * /usr/local/lib
     * /opt/local/lib



2) Modify libvirtd.conf to ensure that the user that runs Karaf has write permissions to libvrit socket.

e.g. set these values in your ~/.libvirt/libvirtd.conf

  unix_sock_ro_perms = "0777"
  unix_sock_rw_perms = "0777"

3) Start the libvirtd

4) Start Karaf (make sure the user that runs Karaf has the required permissions to use libvirt).

5) Add the feature:
   karaf@root> features:addurl mvn:org.fusesource.fabric/fuse-fabric/1.1-SNAPSHOT/xml/features
   karaf@root> features:install virt

6) Create a new virt service using the virt managed service factory:
    The domain name can be anything, for this we'll assume Ubuntu is the name)
    These instructions assume VirtualBox (for the vbox:///session URI)

   karaf@root> config:edit org.fusesource.fabric.virt-Ubuntu
   karaf@root> config:propset url vbox:///session
   karaf@root> config:update

7) Use the shell to list, start or stop domains:
   karaf@root> virt:domain-list

   [Id.] [Name]               [State]
       1 Ubuntu               VIR_DOMAIN_SHUTOFF

   karaf@root> virt:domain-start Ubuntu
   karaf@root> virt:domain-stop Ubuntu

Enjoy!
