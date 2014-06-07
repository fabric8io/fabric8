
## Download

<div class="row">
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-success" href="http://central.maven.org/maven2/io/fabric8/fabric8-karaf/1.1.0.Beta6/fabric8-karaf-1.1.0.Beta6.zip">fabric8-karaf-1.1.0.Beta6.zip</a>
  </div>
  <!-- TODO these are not ready yet
  Note that the [Apache Tomcat](https://tomcat.apache.org/) and [Apache TomEE](http://tomee.apache.org/) distributions are still experimental; the [Apache Karaf](http://karaf.apache.org/) based distro is currently rock solid and complete ;) -->
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-warning" title="Warning!  Expiremental!" href="http://central.maven.org/maven2/io/fabric8/runtime/fabric8-tomcat/1.1.0.Beta6/fabric8-tomcat-1.1.0.Beta6.zip">fabric8-tomcat-1.1.0.Beta6.zip</a>
    <p><small><em>This distro is still expiremental</em></small></p>
  </div>
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-warning" title="Warning!  Expiremental!" href="http://central.maven.org/maven2/io/fabric8/runtime/fabric8-tomee/1.1.0.Beta6/fabric8-tomee-1.1.0.Beta6.zip">fabric8-tomee-1.1.0.Beta6.zip</a>
    <p><small><em>This distro is still expiremental</em></small></p>
  </div>
</div>

## Installation

Unpack the tarball:

    cd ~/Downloads
    unzip fabric8-karaf-1.1.0.Beta6.tar.gz
    cd fabric8-karaf-1.1.0.Beta6

Or you could [build the project](https://github.com/fabric8io/fabric8/blob/master/readme-build.md) with [maven](http://maven.apache.org/) via:

    mvn -DskipTests clean install -Pall
    cd fabric/fabric8-karaf/target
    unzip fabric8-karaf-1.1.0-SNAPSHOT.zip
    cd fabric8-karaf-1.1.0-SNAPSHOT

## Create a fabric

You may wish to edit the user/password in etc/users.properties

Now to create a fabric using the karaf distribution type:

    bin/fabric8

if you are using the Tomcat or TomEE distribution type:

    bin/fabric8 run

Once the container has started up, you have a working Fabric and you can connect to the console

## Use the console

Then open the [hawtio based](http://hawt.io/) console via the [karaf web console on port 8181](http://localhost:8181/) or [web console on port 8080](http://localhost:8080/hawtio/)

Then check out the documentation in the [embedded fabric8 wiki](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles) to guide you through all the available [profiles](#/site/book/doc/index.md?chapter=profiles_md) you can create in a new container or add to an existing container.

Or check out:

 * [how to use the command shell](#/site/book/doc/index.md?chapter=agent_md)
 * [how to use git with fabric8](#/site/book/doc/index.md?chapter=git_md)

### View a demo

To help you get started you could watch one of the demos in the  <a class="btn" href="https://vimeo.com/album/2635012">JBoss Fuse and JBoss A-MQ demo album</a>

For example try this: <a class="btn" href="https://vimeo.com/80625940">JBoss Fuse 6.1 Demo</a>

### Try QuickStarts

New users to Fabric8 is recommended to try some of the [QuickStarts](quickStarts.md).
