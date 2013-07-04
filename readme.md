Open Source, Camel based iPaaS
=============================

Welcome to the Fuse open source stack which consists of:

* [A-MQ](http://fuse.fusesource.org/mq/)
* [Fuse](http://fusesource.com/products/fuse-esb-enterprise/)
* [Fuse Fabric](http://fuse.fusesource.org/fabric/)
* [Fuse Application Bundles](http://fuse.fusesource.org/bundles/)

Here's a Demo
--------------

For slides and videos see [this blog post on the CamelOne 2013 keynote](http://macstrac.blogspot.com/2013/06/introducing-apache-camel-based-open.html).

<iframe src="http://player.vimeo.com/video/68126320" width="500" height="313" frameborder="0" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe> <p><a href="http://vimeo.com/68126320">Camel in the cloud demo from CamelOne 2013</a> from <a href="http://vimeo.com/user18878300">James Strachan</a> on <a href="http://vimeo.com">Vimeo</a>.</p>

Creating a Fabric
-----------------


You can try a [download of the Fuse Fabric code](http://repo.fusesource.com/nexus/content/repositories/jboss-fuse-6.1.x/org/fusesource/fabric/fuse-fabric/) or build the project with maven via:

    cd fabric/fuse-fabric/target
    tar xf fuse-fabric-99-master-SNAPSHOT.tar.gz
    cd fuse-fabric-99-master-SNAPSHOT

From the distro, start up the Fuse container via:

    bin/fusefabric

Once the container starts up, create a Fabric:

    fabric:create -r manualip -m localhost --new-user admin --new-user-password admin

then to enable the [hawtio console](http://hawt.io/):

    container-add-profile root hawtio

you should be able to then open it at [http://localhost:8181/hawtio/](http://localhost:8181/hawtio/)

to add kibana for ElasticSearch based search of logs, metrics & camel messages:

    container-add-profile root kibana

the profile I used in the demo video is **example-camel-fabric** or can be created via the console via:

    container-create-child  --profile example-camel-fabric root mycamel


Running the drools workbench
----------------------------

To demonstrate the provisioning of Tomcat from inside Fuse Fabric along with registering all Tomcat's web apps with the Fabric registry (so we can do cross-application linking easily), try the following:

    container-add-profile root drools-consoles-controller

Now there should be a child Tomcat process running with the drools workbench installed inside it. You can then see it running (give it a minute or so to startup etc):

    ps

If it doens't appear at first, be patient; it takes a little while to download the tomcat & drools workbench distros and get them running (they are not currently pre-cached in the Fuse Fabric distro).

If you run the hawtio application, you should see the Rules tab on the Fabric page; which links to the drools workbench web application running in the child Tomcat container.
