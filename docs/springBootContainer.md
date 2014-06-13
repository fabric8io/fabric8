## Spring Boot container

Fabric comes with a set of features simplifying the effort of running and managing Spring Boot JVM processes. Fabric
Boot utilities and starters are especially useful if you plan to run your system in a microservices-manner backed by
the Spring Boot micro-containers and Fabric-related middleware (Camel, ActiveMQ, CXF and so forth).

### Fabric8 Spring Boot BOM

The best way to manage Spring Boot dependencies in the Fabric8-managed application is to import the Fabric8 Spring Boot
BOM.

    <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>io.fabric8</groupId>
          <artifactId>process-spring-boot-bom</artifactId>
          <version>${fabric8.version}</version>
          <type>pom</type>
          <scope>import</scope>
        </dependency>
      </dependencies>
    </dependencyManagement>

If your POM is armed with the BOM definition presented above, you can import Spring Boot and related Fabric8 jars into
your microservice without specifying the versions of these dependencies.

    <dependencies>
      <dependency>
        <groupId>io.fabric8</groupId>
        <artifactId>process-spring-boot-container</artifactId>
      </dependency>
      <dependency>
        <groupId>io.fabric8</groupId>
        <artifactId>process-spring-boot-starter-camel</artifactId>
      </dependency>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
      </dependency>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
      </dependency>
      <dependency>
        <groupId>org.springframework.data</groupId>
        <artifactId>spring-data-rest-webmvc</artifactId>
      </dependency>
    </dependencies>

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

 In order to specify packages that should be scanned for additional `@Component` and `@Configuration` classes, use
 standard Spring Boot `spring.main.sources` system property. For example if your project `@Configuration` classes are located in
 the `com.example.project` package, you can use the following command to install your jar as a managed process:

      process:install-jar -m io.fabric8.process.spring.boot.container.FabricSpringApplication --jvm-options=-Dspring.main.sources=com.example.project my.group.id my-artifact 1.0

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

### Provisioning Spring Boot applications as Fabric8 Java Containers

The recommended way to provision Spring Boot applications in the Fabric8 environment is to install them as
[Java Containers](http://fabric8.io/gitbook/javaContainer.html). This way your Spring Boot
application will be treated by the Fabric8 as the any other container, so you could take advantage of the Hawtio, ZooKeeper
runtime registry, [Gateway](http://fabric8.io/gitbook/gateway.html) and many other useful Fabric8 features.

In the following paragraphs we will demonstrate how to use the Fabric8 Java Container to provision the
[Spring Boot invoicing microservice demo](https://github.com/fabric8io/fabric8/tree/master/process/process-spring-boot/process-spring-boot-itests/process-spring-boot-itests-service-invoicing).

First of all - create new profile for your application. We will name our profile `invoicing`, because it describes
microservice related to the *invoicing* business concern.

    > profile-create --parents containers-java.spring.boot invoicing

Then add your microservice jar to that profile:

    > profile-edit --pid=io.fabric8.container.java/jarUrl=mvn:io.fabric8/process-spring-boot-itests-service-invoicing/1.1.0-SNAPSHOT invoicing
    Setting value:mvn:io.fabric8/process-spring-boot-itests-service-invoicing/1.1.0-SNAPSHOT key:jarUrl on pid:io.fabric8.container.java and profile:invoicing version:1.0

In order to find `@Component` and `@Configuration` classes specific to your application, set the `spring.main.sources`
system property:

    > profile-edit --pid=io.fabric8.container.java/jvmArguments=-Dspring.main.sources=io.fabric8.process.spring.boot.itests.service.invoicing invoicing
    Setting value:-Dspring.main.sources=io.fabric8.process.spring.boot.itests.service.invoicing key:jvmArguments on pid:io.fabric8.container.java and profile:invoicing version:1.0

Our `invoice` profile is all set now - we can finally provision our microservice as a Java Container:

    > container-create-child --profile invoicing root invoicing
    Starting process
    Running java -Dspring.main.sources=...
    ...
    process is now running (22074)
    The following containers have been created successfully:
    	Container: invoicing.

The container seems to be started properly, but let's verify this by listing REST services available under
`http://localhost:8080` address:

    ~/fabric8-karaf-1.1.0-SNAPSHOT [10001]% curl http://localhost:8080/
    {
      "_links" : {
        "invoice" : {
          "href" : "http://localhost:8080/invoice{?page,size,sort}",
          "templated" : true
        }
      }
    }%

Our invoicing microservice has been successfully started and exposed under the following URL -
`http://localhost:8080/invoice` . As we can see our invoicing service has been deployed as regular Fabric8 child
container.

     > container-list
     [id]                           [version] [connected] [profiles]                                         [provision status]
     root*                          1.0       true        fabric, fabric-ensemble-0000-1                     success
       invoicing                    1.0       true        invoicing                                          success

And at the same time it is visible for the Fabric8 as a managed process:

    > process:ps
    [id]                     [pid] [name]
    invoicing                8576  java io.fabric8.process.spring.boot.container.FabricSpringApplication

### Profile containers-java.spring.boot

You may be wondering what is the `containers-java.spring.boot` profile we used in example above.

    > profile-create --parents containers-java.spring.boot invoicing

This is opinionated base profile for Spring Boot applications provisioned using Fabric8. Why should you use it? Our
Spring Boot profile comes with some useful default settings that we recommend to have in place when deploying Spring
Boot applications.

First of all our `containers-java.spring.boot` profile specifies the main class of your application. It uses
`io.fabric8.process.spring.boot.container.FabricSpringApplication` main class - the opinionated Spring Boot application
configuration for processes provisioned and managed by the Fabric8.

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

Camel starter collects all the `RoutesBuilder` instances from the Spring context and automatically injects
them into the provided `CamelContext`. It means that creating new Camel route with the Spring Boot starter is as simple as
adding the `@Component` annotated class into your classpath:

    @Component
    public class MyRouter extends RouteBuilder {

      @Override
      public void configure() throws Exception {
        from("jms:invoices").to("file:/invoices");
      }

    }

Or creating new route `RoutesBuilder` in your `@Configuration` class:

    @Configuration
    public class MyRouterConfiguration {

      @Bean
      RoutesBuilder myRouter() {
        return new RouteBuilder() {

          @Override
          public void configure() throws Exception {
            from("jms:invoices").to("file:/invoices");
          }

        };
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

