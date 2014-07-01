## Process Management

The **Process Manager** bundle provides support for running *managed processes* on a machine. A *managed process* is a stand alone operating system process which is managed by the Process Manager.

A managed process keeps running if the Process Manager is restarted and it can still start/stop/restart/uninstall the process after it itself is restarted; as the Process Manager knows how to find the underlying operating system process ID (PID) of each managed process.

The Process Manager can run any application; in which case it acts like using init.d, xinit.d, daemontools, monit and other kinds of unix process manager. The difference though is the Process Manager can act at the Fabric8 level since we can use [Fabric Profiles](profiles.html) to determine which machines run which proceses in a fabric.

A *managed process* is similar conceptually to *child containers* in a root Apache Karaf container; each managed process is a separate, stand alone operating system process installed in a sub directory of **${karaf-home}/processes** and is managed by the root container to install/start/stop/restart/uninstall the process.

To users familiar with Apache Karaf, a managed process feels similar to a bundle or feature in a  Karaf container and its interactive shell; the difference being a managed process is a separate, stand alone operating system process.

A process typically has a directory which contains a launcher script according to the [Init Script Actions Specification](http://refspecs.linuxbase.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.


### Deploying JARs as managed processes

The Process Manager also supports turning any Java code (a collection of jars and an executable class name) into a stand alone managed process which can be managed like other operating system processes.

This means you can have fine grained process isolation at the JAR level. Rather than running all your Java code in one big container in a single process, you can decouple executable jars into separate processes all managed as if it were inside a single Java container - will full process isolation and no concerns over potential resource leaks.

One bad managed process will not affect any others and each process can be easily stopped without affecting any others.

This means with Fabric8 you can easily move your Java code between OSGi bundles, [Fuse Bundles](../../bundle/index.html) or *managed processes* depending on your coupling, scaling or process isolation requirements.


### Managing processes like Tomcat, Jetty, HQ Agent

The [ProcessController](https://github.com/fabric8io/fabric8/blob/master/process/process-manager/src/main/java/io/fabric8/process/manager/ProcessController.java#L35) can run any process; though it needs to know exactly how to run it. It assumes the [Init Script Actions Specification](http://refspecs.linuxbase.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.

The default is to use a launch script called **bin/launcher** and then specify a parameter for each command

* bin/launcher start
* bin/launcher stop
* bin/launcher restart
* bin/launcher status
* bin/launcher kill

You can also specify a configuration in JSON for the controller to use:

    process:install -c urlOfJson urlOfTarBall

For example to install Apache Tomcat:

    process-install -c https://raw.github.com/fabric8io/fabric8/blob/master/process/process-manager/src/main/resources/tomcat.json http://apache.favoritelinks.net/tomcat/tomcat-7/v7.0.53/bin/apache-tomcat-7.0.53.tar.gz

then once installed you can start/stop/restart/status it like any other process.

### Kinds of process controller

Process Manager ships with some default **kinds** of controller which lets you use a more concise command to run some common processes.

For example to install an [Apache Tomcat](http://tomcat.apache.org/) distro with the name mycat, in this case [Apache TomEE](http://tomee.apache.org/):

    process:install -k tomcat mycat mvn:org.apache.openejb/apache-tomee/1.5.0/tar.gz/plus

You can use any URL for a distro of Tomcat you wish in the above command. For example you could refer to a specific HTTP URL for a Tomcat distro...

    process:install -k tomcat mycat http://repo2.maven.org/maven2/org/apache/openejb/apache-tomee/1.5.0/apache-tomee-1.5.0-plus.tar.gz

To run [Jetty](http://www.eclipse.org/jetty/):

    process:install -k jetty myjetty http://central.maven.org/maven2/org/eclipse/jetty/jetty-distribution/8.1.4.v20120524/jetty-distribution-8.1.4.v20120524.tar.gz

or

    process:install -k jetty myjetty mvn:org.eclipse.jetty/jetty-distribution/8.1.4.v20120524

Or to install a Fuse HQ Agent

    process:install -k fusehq-agent myagent someURLToDistro


### Working with processes from the Shell

Once a process is installed it given a number (1, 2, 3 etc) which refers to the ID used within the shell to refer to it, to be able to start/stop/restart etc. **Note** that this is not the same thing as the operating system PID!

To view the current installations and their IDs and PIDs use

    ps

You'll then see the simple IDs, the real OS PIDs and the URLs of the installed processes.

Once you know the process number you can then start/stop/restart/status/kill it

    process:start 1
    process:restart 1
    process:status 1

#### Listing all the process-related commands

To see all the available process-related commands type as follows:

    help process

#### Listing environment variables of managed process

In order to display the environment variables assigned by the Fabric8 to the managed process, use
`process:environment <pid>` command.

    > process:environment myProcess
    [Variable]                     [Value]
    FABRIC8_CONTAINER_NAME         sb1
    FABRIC8_HTTP_PORT              8080
    FABRIC8_HTTP_PROXY_PORT        9002
    FABRIC8_SSHD_PROXY_PORT        9000
    FABRIC8_ZOOKEEPER_URL          192.168.122.1:2181

### Installing a jar as a managed process

You can use the **process:install-jar** command to install a jar as a managed process as follows:

    process:install-jar groupId artifactId version

e.g. to create a managed process from this [sample jar](https://github.com/fabric8io/fabric8/tree/master/process/samples/process-sample-camel-spring):

     process:install-jar io.fabric8.samples process-sample-camel-spring 1.1.0

This will then download the jar using the maven coordinates (groupID / artifactId / version) and create a binary installation with the launcher to start/stop/restart the process etc

#### If the jar has no main class

Some jars just contain, say, Spring XML or blueprints and don't contain an executable main. If you need to supply one just specify the **-m** or **--main** options on the command line.

For example:

    process:install-jar -m org.apache.camel.spring.Main io.fabric8.samples process-sample-camel-spring-just-xml 1.1.0

This will then boot up all the Spring XML files in the META-INF/spring/*.xml URI on the classpath.

#### Dependencies

Fabric will download all the dependencies of the installed jar and include them in the classpath of the managed JVM process.
Dependencies are resolved and fetched using Maven dependency resolution mechanism.

### Creating a managed process distro from Java code

See the [example project](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L88) for how you can take any jar with an executable main and turn it into a **tar.gz** which can then be installed directly.

Generally its a case of

* adding the [assembly plugin XML](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L82) to create the tar.gz file using the [process-packaging](https://github.com/fabric8io/fabric8/tree/master/process/process-packaging)
* adding the new tar.gz to the maven build via the [build-helper-maven-plugin](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L90)

So to install the above sample as a tarball use:

    process:install mvn:io.fabric8.samples/process-sample-camel-spring/1.1.0/tar.gz

### Debugging managed processes

Not all your process deployments will succeed with no issues. If your process is not starting successfully, try to
examine the logs of the process you want to start. If digging into logs doesn't help, try to connect to the process with
the debugger build into your IDE.

#### Location of the managed processes data

The managed processes data can be usually found in the `FABRIC8_HOME/processes` directory. Each process uses directory
identified by the numerical ID assigned to it by the process manager. The same numerical identifiers are displayed by the
process manager when you execute `ps` command in the Fabric8 shell.

The listing below demonstrates what you might see in the `processes` directory. Directories `1`, `2` and `3` are the
directories of the particular managed  processes:

     ~/labs/fabric8 % ls instances
     instance.properties 1 2 3

#### Managed process logs

The standard output and standard error streams of the process are redirected to the `PROCESS_DIRECTORY/logs/out.log` and
`PROCESS_DIRECTORY/logs/err.log` respectively. This is usually a good place to start to investigate when your
application doesn't start properly.

#### Connecting to the remote process with the debugger

If investigating process logs isn't enough to identify the issue, you should consider connecting with the debugger to
the managed process. In order to start managed process with the remote debugger port 5005 opened and waiting for you to
connect, execute the following command:

    FABRIC8_JVM_DEBUG=TRUE PROCESS_DIRECTORY/bin/launcher start

If environmental variable `FABRIC8_JVM_DEBUG` is set to `TRUE`, the process will be started with the debugger waiting
for you to connect to the port 5005.

### Process management - test API

Fabric8 comes with the `io.fabric8.process.test.AbstractProcessTest` base class dedicated for testing processes managed 
by Fabric8. `AbstractProcessTest` provides utilities to gracefully start, test and stop managed processes. In order to use
Fabric8 test API, include add the following jar in your project:

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>process-test</artifactId>
      <version>${fabric-version}</version>
      <scope>test</scope>
    </dependency>

Keep in mind that due to the performance reasons you should bootstrap tested process as a singleton static member of
the test class before your test suite starts (you can do it in the `@BeforeClass` block). `AbstractProcessTest` is 
primarily designed to work with the static singleton instances of the processes living as long as the test suite.
The snippet below demonstrates this approach.

    public class InvoicingMicroServiceTest extends AbstractProcessTest {

      static ProcessController processController;

      @BeforeClass
      public static void before() throws Exception {
        processController = processManagerService.installJar(...).getController();
        startProcess(processController);
      }

      @AfterClass
      public static void after() throws Exception {
        stopProcess(processController);
      }

      @Test
      public void shouldDoSomething() throws Exception {
        // test your process here
      }

    }

As you can see in the snippet above, `AbstractProcessTest` comes with the `ProcessManagerService` instance
(`processManagerService`) living for the period of the test class lifespan. Data of the tested processes is stored in
the temporary directory.
