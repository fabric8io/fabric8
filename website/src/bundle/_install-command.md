To install a FAB with some value of _groupId_, _artfactId_, _version_ type the following command into the console of either [Fuse ESB](http://fusesource.com/products/fuse-esb-enterprise/) or [Apache Karaf](http://karaf.apache.org/) (if it has the [Fuse bundle feature installed](faq.html#How_do_I_enable_FAB_support_in_my_OSGi_container_))


{pygmentize:: text}
install fab:mvn:groupId/artifactId/version
{pygmentize}

The **fab:** prefix means treat the given jar as a FAB (Fuse Bundle). This also works with other URIs to _mvn:_ such as _file:_ or _http:_ etc.

Or you can copy a FAB as a file ending in **.fab** (rather than .jar) to the _deploy_ directory of your container, or use the command line shell in Fuse ESB or Karaf


