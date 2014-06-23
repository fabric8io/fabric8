## Process Container

The _Process Container_ is a way to provision and run a stand alone process for cases where you are not using [Docker Containers](http://fabric8.io/gitbook/docker.html).

We actually recommend wherever possible to run [Docker Containers](http://fabric8.io/gitbook/docker.html) since its a powerful abstraction for running isolated processes which are more self contained and easy to run anywhere. Plus increasingly Docker containers are the default packaging of processes which make them run on most infrastructures (IaaS) and platforms as a service (PaaS) environments.

So Process Container is really for when you have not installed docker or wish to run processes outside of Linux on the native host operating system.

### How it works

The Process Container works quite similar to the [Java Containers](http://fabric8.io/gitbook/javaContainer.html); it reuses the underlying [Process Manager](http://fabric8.io/gitbook/processManager.html) abstractions for installing new process instances, starting them, stopping them and checking if they are still running etc.

### Configuring the Process Container

Fabric8 has lots of example profiles using the Process Container in the [containers folder](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers) such as

* [Cassandra](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/services/cassandra.profile)
* [HDFS Name Node](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/services/hdfs.namenode.profile)
* [HDFS Data Node](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/services/hdfs.datanode.profile)
* [Tomcat](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/tomcat.profile)
* [Wildfly](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/wildfly.profile).

If you want to configure how the process container works; such as to configure the distribution or to run post-unpack commands  add your own [io.fabric8.container.process.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/tomcat.profile/io.fabric8.process.java.properties) file to your profile.

You can specify environment variables via: [io.fabric8.environment.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/tomcat.profile/io.fabric8.environment.properties)

You can define the ports that your process will expose via: [io.fabric8.ports.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/tomcat.profile/io.fabric8.ports.properties). For each entry (say FOO) in the file you will have the internal port value specified as the environment variable **FABRIC8_FOO_PORT** and the external port value specified as **FABRIC8_FOO_PROXY_PORT**. e.g. when using Java, Process or Docker containers, the FABRIC8_FOO_PROXY_PORT will be dynamically generated to be unique for each container.

You can overlay any files (e.g. add deployment units and so forth) via [io.fabric8.process.overlay.resources.properties](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/containers/tomcat.profile/io.fabric8.process.overlay.resources.properties)

