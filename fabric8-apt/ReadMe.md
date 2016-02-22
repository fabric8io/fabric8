## Fabric8 APT Tool

This APT plugin generates a JSON Schema file called **io/fabric8/environment/schema.json** inside each jar which uses CDI for your dependency injection and uses the [@ConfigProperty](http://deltaspike.apache.org/documentation/configuration.html) annotation from [deltaspike](http://deltaspike.apache.org/) to inject environment variables or default values into your Java code.

The generated JSON schema document will describe all the environment variables, their types, default values and their description (if you added some javadoc for them).

These JSON schema files will also be used by the [fabric8:json maven goal](mavenplugin.html) to list all of the environment variables and their value in the generated kubernetes JSON file.

#### Viewing all the environment variable injection points

If you have transitive dependencies which include the generated **io/fabric8/environment/schema.json** file in their jars you can view the overall list of environment variable injection points for a project via:

      mvn fabric8:describe-env

This will then list all the environment variables, their default value, type and description.

### Add it to your Maven pom.xml

To enable the fabric8 API tool then add the following to your [Apache Maven](http://maven.apache.org/) pom.xml

    <dependency>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-apt</artifactId>
        <scope>provided</scope>
        <version>2.2.96</version>
    </dependency>

