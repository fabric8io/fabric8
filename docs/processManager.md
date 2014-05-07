## Process Management

The Process Manager bundle provides support for running managed processes on a machine. A managed process is a stand alone operating system process which is managed by the Process Manager.

A managed process keeps running if the Process Manager is restarted and it can still start/stop/restart/uninstall the process after it itself is restarted; as the Process Manager knows how to find the underlying operating system process ID (PID) of each managed process.

The Process Manager can run any application; in which case it acts like using init.d, xinit.d, daemontools, monit and other kinds of unix process manager. The difference though is the Process Manager can act at the Fabric8 level since we can use [Fabric Profiles](http://fabric8.io/#/site/book/doc/index.md?chapter=profiles_md) to determine which machines run which proceses in a fabric.

A managed process is similar conceptually to child containers in a root Apache Karaf container; each managed process is a separate, stand alone operating system process installed in a sub directory of ${karaf-home}/processes and is managed by the root container to install/start/stop/restart/uninstall the process.

To users familiar with Apache Karaf, a managed process feels similar to a bundle or feature in a  Karaf container and its interactive shell; the difference being a managed process is a separate, stand alone operating system process.

A process typically has a directory which contains a launcher script according to the [Init Script Actions Specification](http://refspecs.linuxbase.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.


### Deploying JARs as managed processes

The Process Manager also supports turning any Java code (a collection of jars and an executable class name) into a stand alone managed process which can be managed like other operating system processes.

This means you can have fine grained process isolation at the JAR level. Rather than running all your Java code in one big container in a single process, you can decouple executable jars into separate processes all managed as if it were inside a single Java container - will full process isolation and no concerns over potential resource leaks.

One bad managed process will not affect any others and each process can be easily stopped without affecting any others.

This means with Fabric8 you can easily move your Java code between OSGi bundles, [Fuse Bundles](../../bundle/index.html) or managed processes depending on your coupling, scaling or process isolation requirements.


### Managing processes like Tomcat, Jetty, HQ Agent

The [ProcessController](https://github.com/fabric8io/fabric8/blob/master/process/process-manager/src/main/java/io/fabric8/process/manager/ProcessController.java#L35) can run any process; though it needs to know exactly how to run it. It assumes the [Init Script Actions Specification](http://refspecs.freestandards.org/LSB_3.1.1/LSB-Core-generic/LSB-Core-generic/iniscrptact.html) for starting/stopping/restarting etc.

The default is to use a launch script called bin/launcher and then specify a parameter for each command

 bin/launcher start
 bin/launcher stop
 bin/launcher restart
 bin/launcher status
 bin/launcher kill

You can also specify a configuration in JSON for the controller to use:

    process:install -c urlOfJson urlOfTarBall

For example to install Apache Tomcat:

    process-install -c https://raw.github.com/fabric8io/fabric8/blob/master/process/process-manager/src/main/resources/tomcat.json http://apache.favoritelinks.net/tomcat/tomcat-7/v7.0.53/bin/apache-tomcat-7.0.53.tar.gz

then once installed you can start/stop/restart/status it like any other process.

### Kinds of process controller

Process Manager ships with some default kinds of controller which lets you use a more concise command to run some common processes.

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

Once a process is installed it given a number (1, 2, 3 etc) which refers to the ID used within the shell to refer to it, to be able to start/stop/restart etc. Note that this is not the same thing as the operating system PID!

To view the current installations and their IDs and PIDs use

    ps

You'll then see the simple IDs, the real OS PIDs and the URLs of the installed processes.

Once you know the process number you can then start/stop/restart/status/kill it

    process:start 1
    process:restart 1
    process:status 1

To see all the available commands type

    help process

### Installing a jar as a managed process

You can use the process:install-jar command to install a jar as a managed process as follows:

    process:install-jar groupId artifactId version

e.g. to create a managed process from this [sample jar](https://github.com/fabric8io/fabric8/tree/master/process/samples/process-sample-camel-spring):

     process:install-jar io.fabric8.samples process-sample-camel-spring 1.1.0

This will then download the jar using the maven coordinates (groupID / artifactId / version) and create a binary installation with the launcher to start/stop/restart the process etc

#### If the jar has no main class

Some jars just contain, say, Spring XML or blueprints and don't contain an executable main. If you need to supply one just specify the -m or --main options on the command line.

For example:

    process:install-jar -m org.apache.camel.spring.Main io.fabric8.samples process-sample-camel-spring-just-xml 1.1.0

This will then boot up all the Spring XML files in the META-INF/spring/.xml URI on the classpath.

#### Dependencies

Fabric will download all the dependencies of the installed jar and include them in the classpath of the managed JVM process.
Dependencies are resolved and fetched using Maven dependency resolution mechanism.

### Creating a managed process distro from Java code

See the [example project](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L88) for how you can take any jar with an executable main and turn it into a tar.gz which can then be installed directly.

Generally its a case of

 adding the [assembly plugin XML](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L82) to create the tar.gz file using the [process-packaging](https://github.com/fabric8io/fabric8/tree/master/process/process-packaging)
 adding the new tar.gz to the maven build via the [build-helper-maven-plugin](https://github.com/fabric8io/fabric8/blob/master/process/samples/process-sample-camel-spring/pom.xml#L90)

So to install the above sample as a tarball use:

    process:install mvn:io.fabric8.samples/process-sample-camel-spring/1.1.0/tar.gz

## Process management - Spring Boot support

Fabric comes with a set of features simplifying the effort of running and managing Spring Boot JVM processes. Fabric
Boot utilities and starters are especially useful if you plan to run your system in a microservices-manner backed by
the Spring Boot micro-containers and Fabric-related middleware (Camel, ActiveMQ, CXF and so forth).

### FabricSpringApplication

`FabricSpringApplication` is an executable Java class to be used as a base for the Fabric-managed Spring Boot applications. Its main purpose is to
eliminate the custom code bootstrapping the application, so end-users could create Spring Boot managed process via
Fabric without any custom wiring.

`FabricSpringApplication` can be used in the conjunction with the Fabric Jar Managed Process installer (just
 as demonstrated on the snippet below).

     process:install-jar -m io.fabric8.process.spring.boot.container.FabricSpringApplication my.group.id my-artifact 1.0

 Keep in mind that you don't have to use `FabricSpringApplication` in order to use Fabric goodies for Spring
 Boot (like Fabric starters). However we recommend to use this class as an entry point for your Fabric SpringBoot
 integration, as it implements our opinionated view of the proper Fabric+Boot wiring.

### Embedded FabricSpringApplication

Sometimes you cannot start new Spring Boot JVM process, but instead you have to integrate with the existing web application
or the other piece of legacy Spring software. To support such cases Fabric comes withe the
`EmbeddedFabricSpringApplication`, a bean that can be added to the existing Spring application context in order to start
embedded Fabric Spring Boot context from within it. The embedding context (the one `EmbeddedFabricSpringApplication` has
been added to will become a parent context for the embedded Fabric Spring Boot context.

Creating embedded Fabric application is as simple as that:

    @Bean
    EmbeddedFabricSpringApplication fabricSpringApplication() {
      return new EmbeddedFabricSpringApplication();
    }

For XML configuration the snippet above looks as follows:

    <bean class="io.fabric8.process.spring.boot.container.EmbeddedFabricSpringApplication" />

`EmbeddedFabricSpringApplication` automatically detects its patent Spring `ApplicationContext` and uses it when starting
up new embedded Fabric application.

### Spring Boot Camel starter

Fabric Spring Boot support comes with the opinionated auto-configuration of the Camel context. It provides default
`CamelContext` instance, auto-detects Camel routes available in the Spring context and exposes the key Camel utilities
(like consumer template, producer template or type converter service).

In order to start using Camel with Spring Boot just include `process-spring-boot-starter-camel` jar in your application
classpath.

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>process-spring-boot-starter-camel</artifactId>
      <version>${fabric-version}</version>
    </dependency>

When `process-spring-boot-starter-camel` jar is included in the classpath, Spring Boot will auto-configure the Camel
context for you.

#### Auto-configured CamelContext

The most important piece of functionality provided by the Camel starter is `CamelContext` instance. Fabric Camel starter
will create `SpringCamelContext` for your and take care of the proper initialization and shutdown of that context. Created
Camel context is also registered in the Spring application context (under `camelContext` name), so you can access it just
as the any other Spring bean.

    @Configuration
    public class MyAppConfig {

        @Autowired
        CamelContext camelContext;

        @Bean
        MyService myService() {
          return new DefaultMyService(camelContext);
        }

    }

#### Auto-configured ActiveMQ client

Fabric comes with the auto-configuration class for the ActiveMQ client. It provides a pooled ActiveMQ 
`javax.jms.ConnectionFactory`. In order to use the `ActiveMQAutoConfiguration` in your application just add the 
following jar to your Spring Boot project dependencies:
 
    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>process-spring-boot-starter-activemq</artifactId>
      <version>${fabric-version}</version>
    </dependency>

Fabric's ActiveMQ starter will provide the default connection factory for you.

    @Component
    public class InvoiceReader {
    
      private final ConnectionFactory connectionFactory;
     
      @Autowired
      public InvoiceReader(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
      }
     
      void ProcessNextInvoice() {
        Connection invoiceQueueConnection = connectionFactory.createConnection();
        // invoice processing logic here
      }
     
    }

Keep in mind that if you include the
[spring-jms](http://docs.spring.io/spring/docs/4.0.x/spring-framework-reference/html/remoting.html#remoting-jms) jar
together with the ActiveMQ starter in your Spring Boot application classpath, `JmsTemplate` will be automatically 
populated with the ActiveMQ connection factory.

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>process-spring-boot-starter-activemq</artifactId>
      <version>${fabric-version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
     <artifactId>spring-jms</artifactId>
      <version>${spring-version}</version>
    </dependency>

Now you can use `JmsTemplate` with ActiveMQ without explicit configuration.

    @Component
    public class InvoiceReader {

      private final JmsTemplate jmsTemplate;

      @Autowired
      public InvoiceReader(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
      }
 
      void ProcessNextInvoice() {
        String invoiceId = (String) jmsTemplate.receiveAndConvert("invoices");
        // invoice processing logic
       }
 
    }
