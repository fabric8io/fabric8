## Apache Karaf

Fabric8 provides support for Apache Karaf which is meant to make developing OSGi apps for Kubernetes easier.

Using Fabric8 it is possible to:

* Use environment variable values in Blueprint XML files.
* Use Kubernetes config maps to dynamically update the OSGi config admin
* Provides sensible Kubernetes heath checks for OSGi services

### Adding the Features File

To use any of the features described in document you should add the `fabric8-karaf-features` dependency to your project pom so that it's feature can be installed into your Karaf server.

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>fabric8-karaf-features</artifactId>
      <version>${fabric8.version}</version>
      <classifier>features</classifier>
      <type>xml</type>
    </dependency>

### Fabric8 Blueprint Support

### Fabric8 Config Admin Support

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

          <startupFeatures>
            ...
            <feature>fabric8-karaf-checks</feature>
            ...
          </startupFeatures>

The `fabric8-maven-plugin:resources` goal will detect if your using the `fabric8-karaf-checks` feature and automatically add the Kubernetes for readiness and liveness probes to your container's configuration.

#### Add Custom Heath Checks

You may want to provide additional custom heath checks so that your Karaf server does not start receiving user traffic before it's ready to process the requests.  To do this, you just need to implement the `io.fabric8.karaf.checks.HealthChecker` or `io.fabric8.karaf.checks.ReadinessChecker` interfaces and register those objects in the OSGi registry.

Your project will need to add the following mvn dependency to the project pom.xml:

    <dependency>
      <groupId>io.fabric8</groupId>
      <artifactId>fabric8-karaf-checks</artifactId>
    </dependency>

The easiest way to create and registered an object in the OSGi registry is to use SCR.  Here is an example that performs a health check to make sure you have some free disk space:

    import io.fabric8.karaf.checks.*;
    import org.apache.felix.scr.annotations.*;
    import org.apache.commons.io.FileSystemUtils;
    import java.util.Collections;
    import java.util.List;
    
    @Component(
        name = "example.TimeChecker",
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

