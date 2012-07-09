## Process Management

This module provides libraries and tools for running managed processes as part of [Fuse Fabric](http://fuse.fusesource.org/fabric/index.html).

In addition it provdides tools for turning any Java code (a collection of jars and an executable class name) into a stand alone process which can be managed on Unix like other processes.

A process typically has a directory which contains a launcher script according to the [Init Script Actions Specification](http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.

### Ruby 1.9 Requirements

The current [launcher script](https://github.com/fusesource/fuse/blob/master/process/process-launcher/src/main/distro/bin/launcher.rb#L18) used in the [example project](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/) depends on Ruby 1.9 to be installed.

The launcher is currently designed to run on unixes (Linux / OS X etc).

If you are on OS X and don't have Ruby 1.9 installed then [try install it via RVM](https://rvm.io/rvm/install/)

    curl -L https://get.rvm.io | bash -s stable --ruby

Then in a shell if you type:

    ruby --version

You should get 1.9.0 or later

### Working with processes from the Shell

Perform a local build, then unpack and run the fuse-fabric distro.

Install a new process via

    process:install mvn:org.fusesource.process.samples/process-sample-camel-spring/99-master-SNAPSHOT/tar.gz

Each process is given a number (1, 2, 3 etc) which refers to the ID used within the shell to refer to it, to be able to start/stop/restart etc. **Note** that this is not the same thing as the operating system PID!

Once you know the process number you can then start/stop/restart/status/kill it

    process:start 1
    process:restart 1
    process:status 1

To see all the available commands type

    help process

### Turning Java code into a managed process

See the [example project](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/pom.xml#L82) for how you can take any jar with an executable main and turn it into a tar.gz which can then be installed as shown above.

Generally its a case of

* adding the [assembly plugin XML](https://github.com/fusesource/fuse/blob/master/process/samples/pom.xml#L72) to create the tar.gz file using the [process-packaging](https://github.com/fusesource/fuse/tree/master/process/process-packaging)
* adding the new tar.gz to the maven build via the [build-helper-maven-plugin](https://github.com/fusesource/fuse/blob/master/process/samples/process-sample-camel-spring/pom.xml#L89)