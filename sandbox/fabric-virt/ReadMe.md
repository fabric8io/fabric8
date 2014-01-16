Fuse Fabric and Karaf integration with LibVirt
==============================================

First you'll need to install some virtualisation engine like [VirtualBox](https://www.virtualbox.org/)

Install and configure libvirt
------------------------------

1) Install it using whatever OS mechanism. e.g. on OS X use either

    brew install libvirt

  or

    port install libvirt

  Depending on if you use homebrew or macports

2) Modify libvirtd.conf to ensure that the user that runs Karaf has write permissions to libvrit socket.

e.g. set these values in your libvirtd.conf file which is in either

  * /usr/local/etc/libvirt/libvirtd.conf
  * /opt/local/etc/libvirt/libvirtd.conf

  set these values

    unix_sock_ro_perms = "0777"
    unix_sock_rw_perms = "0777"

3) Start the libvirtd usually you need to be root


Install in vanilla Karaf
------------------------

We recommend you use the Fuse Fabric distro which comes mostly pre-configured. Here's the steps you need to run if you are in a vanilla Karaf:

1) You need to add jna.library.path to etc/system.properties pointing to the installtion location of libvirt lib directory.
  This is probably in a directory like the following:

   * /usr/local/lib
   * /opt/local/lib

2) Add the feature URL

    karaf@root> features:addurl mvn:io.fabric8/fabric8-karaf/1.1-SNAPSHOT/xml/features


Install in Fuse Fabric distro
----------------------------

1) Start Fuse Fabric or Karaf (make sure the user that runs Karaf has the required permissions to use libvirt).

2) Add the feature:

    karaf@root> features:install virt

3) Create a new virt service using the virt managed service factory:
  The domain name can be anything, for this we'll assume Ubuntu is the name)
  These instructions assume VirtualBox (for the vbox:///session URI)

    karaf@root> config:edit io.fabric8.virt-Ubuntu
    karaf@root> config:propset url vbox:///session
    karaf@root> config:update

4) Use the shell to list, start or stop domains:

     karaf@root> virt:domain-list

    [Id.] [Name]               [State]
       1 Ubuntu               VIR_DOMAIN_SHUTOFF

    karaf@root> virt:domain-start Ubuntu
    karaf@root> virt:domain-stop Ubuntu

Enjoy!
