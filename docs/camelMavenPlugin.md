## Fabric8 Camel Maven Plugin

This maven plugin makes it possible to run some of the [Forge](forge.md) commands from Maven command line.


### Goals

For building and pushing docker images

* `fabric8-camel:validate` validates the Maven project source code to identify invalid Camel endpoint uris

### Adding the plugin to your project

To enable this plugin add the following to your pom.xml:

      <plugin>
        <groupId>io.fabric8.forge</groupId>
        <artifactId>fabric8-camel-maven-plugin</artifactId>
        <version>${fabric8.forge.version}</version>
      </plugin>

Notice the version of the maven plugin is the fabric8-forge version, and not the fabric8 version. These two projects have different release numbers.

Then you can run the validate goal from the command line or from within your Java editor such as IDEA or Eclipse.

     mvn fabric8-camel:validate

You can also enable the plugin to automatic run as part of the build to catch these errors.

      <plugin>
        <groupId>io.fabric8.forge</groupId>
        <artifactId>fabric8-camel-maven-plugin</artifactId>
        <version>${fabric8.forge.version}</version>
        <executions>
          <execution>
            <id>validate</id>
            <goals>
              <goal>validate</goal>
            </goals>
          </execution>
        </executions>
      </plugin>

TODO: Add the phase stuff


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
</table>