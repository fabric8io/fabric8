## Fabric8 Camel Maven Plugin

This maven plugin makes it possible to run some of the [Forge](forge.md) commands from Maven command line.


### Goals

For validating Camel endpoints in the source code:

* `fabric8-camel:validate` validates the Maven project source code to identify invalid Camel endpoint uris

### Adding the plugin to your project

To enable this plugin add the following to your pom.xml:

      <plugin>
        <groupId>io.fabric8.forge</groupId>
        <artifactId>fabric8-camel-maven-plugin</artifactId>
        <version>2.2.129</version>
      </plugin>

Notice the version number (current 2.2.129) is the fabric8-forge release. You can find the [latest release number](https://github.com/fabric8io/fabric8-forge/releases) on github. 

Then you can run the validate goal from the command line or from within your Java editor such as IDEA or Eclipse.

     mvn fabric8-camel:validate

You can also enable the plugin to automatic run as part of the build to catch these errors.

      <plugin>
        <groupId>io.fabric8.forge</groupId>
        <artifactId>fabric8-camel-maven-plugin</artifactId>
        <version>2.2.129</version>
        <executions>
          <execution>
            <phase>process-classes</phase>      
            <goals>
              <goal>validate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

The phase determines when the plugin runs. In the sample above the phase is `process-classes` which runs after the compilation of the main source code.

The maven plugin can also be configured to validate the test source code , which means that the phase should be changed accordingly to `process-test-classes` as shown below:

      <plugin>
        <groupId>io.fabric8.forge</groupId>
        <artifactId>fabric8-camel-maven-plugin</artifactId>
        <version>2.2.129</version>
        <executions>
          <execution>
            <configuration>
              <includeTest>true</includeTest>
            </configuration>
            <phase>process-test-classes</phase>      
            <goals>
              <goal>validate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>


### Running the goal on any Maven project

You can also run the validate goal on any Maven project without having to add the plugin to the `pom.xml` file. Doing so requires to specify the plugin using its fully qualified name. For example to run the goal on the camel-example-cdi from Apache Camel you can run

    $cd camel-example-cdi
    $mvn io.fabric8.forge:fabric8-camel-maven-plugin:2.2.129:validate

which then runs and outputs the following:

```
[INFO] ------------------------------------------------------------------------
[INFO] Building Camel :: Example :: CDI 2.16.2
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- fabric8-camel-maven-plugin:2.2.129:validate (default-cli) @ camel-example-cdi ---
[INFO] Endpoint validation success: (4 = passed, 0 = invalid, 0 = incapable, 0 = unknown components)
[INFO] Simple validation success: (0 = passed, 0 = invalid)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

The validation passed, and 4 endpoints was validated. Now suppose we made a typo in one of the Camel endpoint uris in the source code, such as:

    @Uri("timer:foo?period=5000")

is changed to include a typo error in the `period` option

    @Uri("timer:foo?perid=5000")

And when running the validate goal again reports the following:

```
[INFO] ------------------------------------------------------------------------
[INFO] Building Camel :: Example :: CDI 2.16.2
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- fabric8-camel-maven-plugin:2.2.129:validate (default-cli) @ camel-example-cdi ---
[WARNING] Endpoint validation error at: org.apache.camel.example.cdi.MyRoutes(MyRoutes.java:32)

	timer:foo?perid=5000

	                   perid    Unknown option. Did you mean: [period]


[WARNING] Endpoint validation error: (3 = passed, 1 = invalid, 0 = incapable, 0 = unknown components)
[INFO] Simple validation success: (0 = passed, 0 = invalid)
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```


### Options

The maven plugin supports the following options which can be configured from the command line, or defined in the `pom.xml` file in the `<configuration>` tag.

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Default Value</th>
<th>Description</th>
</tr>
<tr>
<td>failOnError</td>
<td>false</td>
<td>Whether to fail if invalid Camel endpoints was found. By default the plugin logs the errors at WARN level</td>
</tr>
<tr>
<td>logUnparseable</td>
<td>false</td>
<td>Whether to log endpoint URIs which was un-parsable and therefore not possible to validate</td>
</tr>
<tr>
<td>includeJava</td>
<td>true</td>
<td>Whether to include Java files to be validated for invalid Camel endpoints</td>
</tr>
<tr>
<td>includeXml</td>
<td>true</td>
<td>Whether to include XML files to be validated for invalid Camel endpoints</td>
</tr>
<tr>
<td>includeTest</td>
<td>false</td>
<td>Whether to include test source code</td>
</tr>
<tr>
<td>includes</td>
<td></td>
<td>To filter the names of java and xml files to only include files matching any of the given list of patterns (wildcard and regular expression). Multiple values can be separated by comma.</td>
</tr>
<tr>
<td>excludes</td>
<td></td>
<td>To filter the names of java and xml files to exclude files matching any of the given list of patterns (wildcard and regular expression). Multiple values can be separated by comma.</td>
</tr>
<tr>
<td>ignoreUnknownComponent</td>
<td>true</td>
<td>Whether to ignore unknown components</td>
</tr>
<tr>
<td>ignoreIncapable</td>
<td>true</td>
<td>Whether to ignore incapable of parsing the endpoint uri</td>
</tr>
<tr>
<td>ignoreLenientProperties</td>
<td>true</td>
<td>Whether to ignore components that uses lenient properties. When this is true, then the uri validation is stricter but would fail on properties that are not part of the component but in the uri because of using lenient properties. For example using the HTTP components to provide query parameters in the endpoint uri.</td>
</tr>
<tr>
<td>showAll</td>
<td>false</td>
<td>Whether to show all endpoints and simple expressions (both invalid and valid).</td>
</tr>
</table>