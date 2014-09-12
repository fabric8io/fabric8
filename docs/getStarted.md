## Getting Started

### Download

<div class="row">
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-success" href="https://repo1.maven.org/maven2/io/fabric8/fabric8-karaf/1.2.0.Beta4/fabric8-karaf-1.2.0.Beta4.zip">fabric8-karaf-1.2.0.Beta4.zip</a>
  </div>
  <!-- TODO these are not ready yet
  Note that the [Apache Tomcat](https://tomcat.apache.org/) and [Apache TomEE](http://tomee.apache.org/) distributions are still experimental; the [Apache Karaf](http://karaf.apache.org/) based distro is currently rock solid and complete ;) -->
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-warning" title="Warning!  Experimental!" href="https://repo1.maven.org/maven2/io/fabric8/runtime/fabric8-tomcat/1.2.0.Beta4/fabric8-tomcat-1.2.0.Beta4.zip">fabric8-tomcat-1.2.0.Beta4.zip</a>
    <p><small><em>This distro is still experimental</em></small></p>
  </div>
  <div class="col-md-4 text-center">
    <a class="btn btn-large btn-warning" title="Warning!  Experimental!" href="https://repo1.maven.org/maven2/io/fabric8/runtime/fabric8-tomee/1.2.0.Beta4/fabric8-tomee-1.2.0.Beta4.zip">fabric8-tomee-1.2.0.Beta4.zip</a>
    <p><small><em>This distro is still experimental</em></small></p>
  </div>
</div>

If you've used a previous version, you may want to check out the [Change Log](http://fabric8.io/changes/index.html)

### Installation

Unpack the tarball:

    cd ~/Downloads
    unzip fabric8-karaf-1.2.0.Beta4.zip
    cd fabric8-karaf-1.2.0.Beta4

Or, [build the project](https://github.com/fabric8io/fabric8/blob/master/readme-build.md) with [Maven](http://maven.apache.org/) via:

    MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
    mvn -DskipTests clean install -Pall
    cd fabric/fabric8-karaf/target
    unzip fabric8-karaf-1.2.0-SNAPSHOT.zip
    cd fabric8-karaf-1.2.0-SNAPSHOT

### Create a fabric

By default, fabric starts up with a default admin user with username `admin` and password `admin`.
This can be changed by editing the `etc/users.properties` file before starting fabric for the first time.

Now to create a fabric using the karaf distribution type:

    bin/fabric8

If you are using the Tomcat or TomEE distribution type:

    bin/fabric8 run

Once the container has started up, you have a working Fabric and you can connect to the console.

### Use the web console

Then open the [hawtio based](http://hawt.io/) console via the Karaf web console, which runs on [port 8181](http://localhost:8181/):

    http://localhost:8181/

Then check out the documentation in the [embedded fabric8 wiki](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles) to guide you through all the available [profiles](profiles.html) you can create in a new container or add to an existing container.

Or check out:

 * [how to use the command shell](http://fabric8.io/gitbook/agent.html)
 * [how to use git with fabric8](http://fabric8.io/gitbook/git.html)

#### Web console for Tomcat or TomEE distributions

If you are using the Tomcat or TomEE distribution, then the console is on [port 8080](http://localhost:8080/hawtio/), and the console is deployed under the `hawtio` context path. The url for the web console is therefore:

    http://localhost:8080/hawtio/   

#### View a demo

To help you get started, you could watch one of the demos in the  <a class="btn btn-success" href="https://vimeo.com/album/2635012">JBoss Fuse and JBoss A-MQ demo album</a>

For example, try the <a class="btn btn-success" href="https://vimeo.com/80625940">JBoss Fuse 6.1 Demo</a>

#### Try QuickStarts

New users to Fabric8 should try the [QuickStarts](http://fabric8.io/gitbook/quickstarts.html).

#### Read the documentation

Check out the [Overview](http://fabric8.io/gitbook/overview.html) and [User Guide](http://fabric8.io/gitbook/index.html).
