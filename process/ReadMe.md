## Process Management

The **process-manager** bundle provides support for running managed processes as part of [Fuse Fabric](http://fuse.fusesource.org/fabric/index.html).

In addition it provides tools for turning any Java code (a collection of jars and an executable class name) into a stand alone process which can be managed on Unix like other processes.

A process typically has a directory which contains a launcher script according to the [Init Script Actions Specification](http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.

### Running processes like Tomcat, Jetty, HQ Agent

The [ProcessController](https://github.com/fusesource/fuse/blob/master/process/process-manager/src/main/java/org/fusesource/process/manager/ProcessController.java#L34) can run any process; though it needs to know exactly how to run it. It assumes the [Init Script Actions Specification](http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.

The default is to use a launch script called **bin/launcher** and then specify a parameter for each command

* bin/launcher start
* bin/launcher stop
* bin/launcher restart
* bin/launcher status
* bin/launcher kill

You can also specify a configuration in JSON for the controller to use:

    process:install -c urlOfJson urlOfTarBall

For example to install Apache Tomcat:

    process-install -c https://raw.github.com/fusesource/fuse/master/process/process-manager/src/main/resources/tomcat.json http://apache.favoritelinks.net/tomcat/tomcat-7/v7.0.29/bin/apache-tomcat-7.0.29.tar.gz

then once installed you can start/stop/restart/status it like any other process.

### Kinds of process controller

Process Manager ships with some default **kinds** of controller which lets you use a more concise command to run some common processes.

For example to install an Apache Tomcat:

    process:install -k tomcat http://apache.favoritelinks.net/tomcat/tomcat-7/v7.0.29/bin/apache-tomcat-7.0.29.tar.gz

To run Jetty:

    process:install -k jetty http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/8.1.4.v20120524/jetty-distribution-8.1.4.v20120524.tar.gz

or

    process:install -k jetty mvn:org.eclipse.jetty/jetty-distribution/8.1.4.v20120524

Or to install a Fuse HQ Agent

    process:install -k fusehq-agent someURLToDistro


### Working with processes from the Shell

Once a process is installed it given a number (1, 2, 3 etc) which refers to the ID used within the shell to refer to it, to be able to start/stop/restart etc. **Note** that this is not the same thing as the operating system PID!

To view the current installations and their IDs and PIDs use

    ps

You'll then see the simple IDs, the real OS PIDs and the URLs of the installed processes.

Once you know the process number you can then start/stop/restart/status/kill it

    process:start 1
    process:restart 1
    process:status 1

To see all the available commands type

    help process

## Installing a jar as a managed process

You can use the **process:install-jar** command to install a jar as a managed process as follows:

    process:install-jar groupId artifactId version

e.g. to create a managed process from this [sample jar](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring):

     process:install-jar org.fusesource.process.samples process-sample-camel-spring 99-master-SNAPSHOT

This will then download the jar using the maven coordinates (groupID / artifactId / version) and create a binary installation with the launcher to start/stop/restart the process etc

Note that currently the jar based managed processes are dependent on **Ruby 1.9** being installed:

### Ruby 1.9 Requirements

The current [launcher script](https://github.com/fusesource/fuse/blob/master/process/process-launcher/src/main/distro/bin/launcher.rb#L18) used in the [example project](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/) depends on Ruby 1.9 to be installed.

The launcher is currently designed to run on unixes (Linux / OS X etc).

If you are on OS X and don't have Ruby 1.9 installed then [try install it via RVM](https://rvm.io/rvm/install/)

    curl -L https://get.rvm.io | bash -s stable --ruby

Then in a shell if you type:

    ruby --version

You should get 1.9.0 or later

### Creating a managed process distro from Java code

See the [example project](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/pom.xml#L82) for how you can take any jar with an executable main and turn it into a **tar.gz** which can then be installed directly.

Generally its a case of

* adding the [assembly plugin XML](https://github.com/fusesource/fuse/blob/master/process/samples/pom.xml#L72) to create the tar.gz file using the [process-packaging](https://github.com/fusesource/fuse/tree/master/process/process-packaging)
* adding the new tar.gz to the maven build via the [build-helper-maven-plugin](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/pom.xml#L89)

So to install the above sample as a tarball use:

    process:install mvn:org.fusesource.process.samples/process-sample-camel-spring/99-master-SNAPSHOT/tar.gz
