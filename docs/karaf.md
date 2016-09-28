## Apache Karaf

Fabric8 provides support for Apache Karaf which is meant to make developing OSGi apps for Kubernetes easier.

Using Fabric8 it is possible to:

* Use a number of strategies to resolve placeholders in Blueprint XML files, like:
  * Environment variables
  * System properties
  * Services
  * Kubernetes ConfigMap
  * Kubernetes Secrets
* Use Kubernetes config maps to dynamically update the OSGi config admin
* Provides sensible Kubernetes heath checks for OSGi services

### Adding the Features File

To use any of the features described in document you should add the `fabric8-karaf-features` dependency to your project pom so that it's feature can be installed into your Karaf server.

```xml
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>fabric8-karaf-features</artifactId>
  <version>${fabric8.version}</version>
  <classifier>features</classifier>
  <type>xml</type>
</dependency>
```

### Fabric8 Karaf Core

The bundle fabric8-karaf-core provides functionalities used by Blueprint and Config Admin extensions.

To include the feature in your custom Karaf distribution, add it to startupFeatures in your project's pom.xml:

```xml
<startupFeatures>
  ...
  <feature>fabric8-karaf-core</feature>
  ...
</startupFeatures>
```

#### Property placeholders resolvers

fabric8-karaf-core exports a service with the following interface:

```java
public interface PlaceholderResolver {
    /**
     * Resolve a placeholder using the strategy indicated by the prefix
     *
     * @param value the placeholder to resolve
     * @return the resolved value or null if not resolved
     */
    String resolve(String value);

    /**
     * Replaces all the occurrences of variables with their matching values from the resolver using the given source string as a template.
     *
     * @param source the string to replace in
     * @return the result of the replace operation
     */
    String replace(String value);

    /**
     * Replaces all the occurrences of variables within the given source builder with their matching values from the resolver.
     *
     * @param value the builder to replace in
     * @rerurn true if altered
     */
    boolean replaceIn(StringBuilder value);

    /**
     * Replaces all the occurrences of variables within the given dictionary
     *
     * @param dictionary the dictionary to replace in
     * @rerurn true if altered
     */
    boolean replaceAll(Dictionary<String, Object> dictionary);

    /**
     * Replaces all the occurrences of variables within the given dictionary
     *
     * @param dictionary the dictionary to replace in
     * @rerurn true if altered
     */
    boolean replaceAll(Map<String, Object> dictionary);
}
```

The PlaceholderResolver service acts as a collector for different property placeholder resolution strategies and by default it provides:

| Prefix       | Example                  | Description
| ------------ | ------------------------ | ---
| env          | env:JAVA_HOME            | to lookup the property from OS environment variables.
| sys          | sys:java.version         | to lookup the property from Java JVM system properties.
| service      | service:amq              | to lookup the property from OS environment variables using the service naming idiom.
| service.host | service.host:amq         | to lookup the property from OS environment variables using the service naming idiom returning the hostname part only.
| service.port | service.port:amq         | to lookup the property from OS environment variables using the service naming idiom returning the port part only.
| k8s:map      | k8s:map:myMap/myKey      | to lookup the property from a Kubernetes ConfigMap
| k8s:secret   | k8s:secrets:amq/password | to lookup the property from a Kubernetes Secrets

</br>

The property placeholder service supports the following options:

| Name                          | Default     | Description
| ----------------------------- | ----------- | -----------
| fabric8.placeholder.prefix    | $[          | The prefix for the placeholder
| fabric8.placeholder.suffix    | ]           | The suffix for the placeholder

Note:
  * Options can be set via system properties and/or environment variables

</br>  


#### Add a custom property placeholders resolvers

You may need to add a custom placeholder resolver to support a specific need (i.e. custom encryption) and you can leverage PlaceholderResolver service to make such resolver available to Blueprint and ConfigAdmin with a single implementation.
To do so, you will need to add the following mvn dependency to the project pom.xml:

```xml
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>fabric8-karaf-core</artifactId>
</dependency>
```

You then need to implement the [PropertiesFunction](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-karaf/fabric8-karaf-core/src/main/java/io/fabric8/karaf/core/properties/function/PropertiesFunction.java) interface and register it as OSGi service (the easiest way is to use SCR).

```java
import io.fabric8.karaf.core.properties.function.PropertiesFunction;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Service;

@Component(
    immediate = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Service(PropertiesFunction.class)
public class MyPropertiesFunction implements PropertiesFunction {
    @Override
    public String getName() {
        return "myResolver";
    }

    @Override
    public String apply(String remainder) {
        // Parse and resolve remainder        
        return remainder;
    }
}
```

You can then reference your resolver in CM like:
```properties
my.property = $[myResolver:value-to-resolve]
```

Note:
  * Details about Blueprint/CM integration will be shown in the following sections

### Fabric8 Karaf Config Admin Support

To include the feature in your custom Karaf distribution, add it to startupFeatures in your project's pom.xml:

```xml
<startupFeatures>
  ...
  <feature>fabric8-karaf-cm</feature>
  ...
</startupFeatures>
```

#### ConfigMap injection

fabric8-karaf-cm provides a ConfigAdmin bridge that inject ConfigMap values in Karaf's ConfigAdmin.

To be taken into account by the ConfigAdmin bridge, a ConfigMap has to be labeled with "karaf.pid", where its values corresponds to the pid of your component:

```yaml
kind: ConfigMap
apiVersion: v1
metadata:
  name: myconfig
  namespace: default
  labels:
    karaf.pid: com.mycompany.bundle
data:
  example.property.1: my property one
  example.property.2: my property two

```

Individual properties work fine for most cases but sometimes you may want more freedom to define your configuration sou you can use a single property names as it would be your pid file in karaf/etc:


```yaml
kind: ConfigMap
apiVersion: v1
metadata:
  name: myconfig
  namespace: default
  labels:
    karaf.pid: com.mycompany.bundle
data:
  com.mycompany.bundle.cfg: |
    example.property.1: my property one
    example.property.2: my property two
```
</br>

#### Configuration plugin

fabric8-karaf-cm provides a ConfigurationPlugin which resolves configuration's property placeholders before they reach your service so you do not need any special configuration to resolve placeholders in Blueprint XML or in any Managed Service.

Example:

* my.service.cfg

    ```properties
    amq.usr = $[k8s:secret:$[env:ACTIVEMQ_SERVICE_NAME]/username]
    amq.pwd = $[k8s:secret:$[env:ACTIVEMQ_SERVICE_NAME]/password]
    amq.url = tcp://$[env+service:ACTIVEMQ_SERVICE_NAME]
    ```

* my-service.xml

    ```xml
    <?xml version="1.0" encoding="UTF-8"?>

    <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:cm="http://aries.apache.org/blueprint/xmlns/blueprint-cm/v1.1.0"
               xsi:schemaLocation="
                 http://www.osgi.org/xmlns/blueprint/v1.0.0
                 https://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
                 http://camel.apache.org/schema/blueprint
                 http://camel.apache.org/schema/blueprint/camel-blueprint.xsd">

      <cm:property-placeholder persistent-id="my.service" id="my.service" update-strategy="reload"/>

      <bean id="activemq" class="org.apache.activemq.camel.component.ActiveMQComponent">
         <property name="userName"  value="${amq.usr}"/>
         <property name="password"  value="${amq.pwd}"/>
         <property name="brokerURL" value="${amq.url}"/>
      </bean>
    </blueprint>
    ```

</br>

Fabric8 Karaf Config Admin supports the following options:

| Name                          | Default     | Description
| ----------------------------- | ----------- | -----------
| fabric8.config.plugin.enabled | false       | Enable ConfigurationPlugin
| fabric8.cm.bridge.enabled     | true        | Enable ConfigAdmin bridge
| fabric8.config.watch          | true        | Enable watching for ConfigMap changes
| fabric8.config.merge          | false       | Enable merge ConfigMap values in ConfigAdmin
| fabric8.config.meta           | true        | Enable injecting ConfigMap meta in ConfigAdmin bridge
| fabric8.pid.label             | karaf.pid   | Define the label the ConfigAdmin bridge looks for



Notes:
  * Otions can be set via system properties and/or environment variables
  * ConfigurationPlugin requires Aries Bleuprint CM 1.0.9 or above
  * To use ConfigMap as source of properties on OpenShift, the OSE service account needs to have at least view role added.

  ```
  oc policy add-role-to-user view system:serviceaccount:<namespace>:default
  ```

</br>

### Fabric8 Karaf Blueprint Support


fabric8-karaf-blueprint leverages [Aries PropertyEvaluator](https://github.com/apache/aries/blob/trunk/blueprint/blueprint-core/src/main/java/org/apache/aries/blueprint/ext/evaluator/PropertyEvaluator.java) and property placeholders resolvers from fabric8-karaf-core to let you resolve placeholders in your Blueprint XML file.

To include the feature in your custom Karaf distribution, add it to startupFeatures in your project's pom.xml:

```xml
<startupFeatures>
  ...
  <feature>fabric8-karaf-blueprint</feature>
  ...
</startupFeatures>
```

The fabric8 evaluator supports chained evaluators i.e ${env+service:MY_ENV_VAR} where the first step is to resolve MY_ENV_VAR against environment variables then the result is resolved using service function.

Example:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:ext="http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.2.0"
           xsi:schemaLocation="
             http://www.osgi.org/xmlns/blueprint/v1.0.0
             https://www.osgi.org/xmlns/blueprint/v1.0.0/blueprint.xsd
             http://camel.apache.org/schema/blueprint
             http://camel.apache.org/schema/blueprint/camel-blueprint.xsd
             http://aries.apache.org/blueprint/xmlns/blueprint-ext/v1.3.0
             http://aries.apache.org/schemas/blueprint-ext/blueprint-ext-1.3.xsd">

  <ext:property-placeholder evaluator="fabric8" placeholder-prefix="$[" placeholder-suffix="]"/>

  <bean id="activemq" class="org.apache.activemq.camel.component.ActiveMQComponent">
     <property name="userName"  value="$[k8s:secret:$[env:ACTIVEMQ_SERVICE_NAME]/username]"/>
     <property name="password"  value="$[k8s:secret:$[env:ACTIVEMQ_SERVICE_NAME]/password]"/>
     <property name="brokerURL" value="tcp://$[env+service:ACTIVEMQ_SERVICE_NAME]"/>
  </bean>
</blueprint>
```

Notes:
  * Nested property placeholder substitution requires Aries Blueprint Core 1.7.0 or above

### Fabric8 Karaf Health Checks

It is recommended you always install the `fabric8-karaf-checks` as a startup feature. With it enabled, your Karaf server will expose
`http://0.0.0.0:8181/readiness-check` and `http://0.0.0.0:8181/health-check` URLs which can be used by Kubernetes for readiness and liveness probes.  Those URLs will only respond with a HTTP 200 status code when the following is true:

* OSGi Framework is started
* All OSGi bundles are started
* All boot features are installed
* All deployed BluePrint bundles are in the created state.
* All deployed SCR bundles are in the active, registered or factory state.
* All web bundles are deployed to the web server.
* All created Camel contexts are in the started state.

Adding the feature to your project's pom.xml:

```xml
<startupFeatures>
  ...
  <feature>fabric8-karaf-checks</feature>
  ...
</startupFeatures>
```

The `fabric8-maven-plugin:resources` goal will detect if your using the `fabric8-karaf-checks` feature and automatically add the Kubernetes for readiness and liveness probes to your container's configuration.

#### Add Custom Heath Checks

You may want to provide additional custom heath checks so that your Karaf server does not start receiving user traffic before it's ready to process the requests.  To do this, you just need to implement the `io.fabric8.karaf.checks.HealthChecker` or `io.fabric8.karaf.checks.ReadinessChecker` interfaces and register those objects in the OSGi registry.

Your project will need to add the following mvn dependency to the project pom.xml:

```xml
<dependency>
  <groupId>io.fabric8</groupId>
  <artifactId>fabric8-karaf-checks</artifactId>
</dependency>
```

The easiest way to create and registered an object in the OSGi registry is to use SCR.  Here is an example that performs a health check to make sure you have some free disk space:

```java
import io.fabric8.karaf.checks.*;
import org.apache.felix.scr.annotations.*;
import org.apache.commons.io.FileSystemUtils;
import java.util.Collections;
import java.util.List;

@Component(
    name = "example.DiskChecker",
    immediate = true,
    enabled = true,
    policy = ConfigurationPolicy.IGNORE,
    createPid = false
)
@Service({HealthChecker.class, ReadinessChecker.class})
public class DiskChecker implements HealthChecker, ReadinessChecker {

    public List<Check> getFailingReadinessChecks() {
        // lets just use the same checks for both readiness and health
        return getFailingHeathChecks();
    }

    public List<Check> getFailingHeathChecks() {
        long free = FileSystemUtils.freeSpaceKb("/");
        if (free < 1024 * 500) {
            return Collections.singletonList(new Check("disk-space-low", "Only " + free + "kb of disk space left."));
        }
        return Collections.emptyList();
    }
}
```
