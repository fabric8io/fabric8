# Fabric8 Maven Plugin

This maven plugin makes it easy to create or update a fabric profile from your maven project.

When you deploy your project to a fabric profile with this plugin the following takes place:

* uploads any artifacts into the fabric's maven repository
* lazily creates the fabric profile or version you specify
* adds/updates the maven project artifact into the profile configuration
* adds any additional parent profile, bundles or features to the profile.

## Configuring the plugin

First you will need to edit your **~/.m2/settings.xml** file to add the fabric server's user and password so that the maven plugin can login to the fabric..

e.g. add this to the &lt;servers&gt; element:

    <server>
      <id>fabric8.upload.repo</id>
      <username>admin</username>
      <password>admin</password>
    </server>

If you don't do this, the first time you use the fabric8 plugin it will ask you if you wish to update your ~/.m2/settings.xml file and prompt you for the information; then update it.

The default fabric upload maven repo ID is **fabric8.upload.repo**. You can define as many &lt;server&gt; elements in your settings file as you like for each of the fabrics you wish to work with. Then to pick the credentials to use for a server specify the server id as the **serverId** property on the fabric8 maven plugin configuration section (see below) or use the **fabric8.serverId** maven property.

## Using the plugin

To use the fabric8 maven plugin to deploy into a fabric profile on any maven project just type:

    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

If you have added the fabric8 maven plugin to your pom.xml as follows:

    <plugins>
      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
      </plugin>
    </plugins>

Then you can use the more concise maven goal instead:

    mvn fabric8:deploy

The project will then be built and deployed into a profile in the fabric. By default the profile is named **$group-$artifact** but you can override the profile ID, version ID and location of the Fabric Jolokia URL via the maven plugin configuration or via the command line.

e.g. to try it out

    cd quickstarts/rest
    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

Then you should see this profile being created at the [my-rest/rest profile page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/my/rest.profile) which should have a bundle and some features added too (click on the Bundle and Feature tabs and you should see those).

## Specifying the profile information in the plugin configuration

You can configure the maven plugin to explicitly specify the profile to create via the plugin &lt;configuration&gt; section in your pom.xml:

    <plugins>
      <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <configuration>
            <profile>my-thing</profile>
          </configuration>
      </plugin>
    </plugins>

Or you can use maven properties to configure the plugin, which provides a more flexible way to map multi-module projects in maven onto profiles in fabric8.

For example if you have a multi-module maven project like this:

    pom.xml
    foo/
      pom.xml
      a/pom.xml
      b/pom.xml
      ...
    bar/
      pom.xml
      c/pom.xml
      d/pom.xml
      ...

then in the root pom.xml you might want to define the mvn plugin once like this:

    <plugins>
      <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
      </plugin>
    </plugins>

Then in foo/pom.xml you can define the **fabric8.profile** property:

    <project>
      ...
      <properties>
        <fabric8.profile>my-foo</fabric8.profile>
        ...
      </properties>
      ...

Then all of the projects within the foo folder, such as foo/a and foo/b would all deploy to the same profile (in this case profile **my-foo**). You can use the same approach to put all of the projects inside the bar folder into a different profile too.

At any point in your tree of maven projects you can define a maven **fabric.profile** property to specify exactly where it gets deployed; along with any other property on the plugin (see the Property Reference below).

## Specifying features, additional bundles, repositories and parent profiles

You can also specify additional configuration in the maven plugin like this:

    <plugins>
      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
        <configuration>
          <profile>my-rest</profile>
          <features>fabric-cxf-registry fabric-cxf cxf war swagger</features>
          <featureRepos>mvn:org.apache.cxf.karaf/apache-cxf/${version:cxf}/xml/features</featureRepos>
        </configuration>
      </plugin>
    </plugins>

Notice we can pass in a space-separated list of features to include in the profile.

We've used space separated lists for the parent profile IDs, features, repositories and bundles so that its easy to reuse maven properties for these values (for example to add some extra features in a child maven project while inheriting from the parent project).

### Specifying configuration using maven properties

You can also use maven property values (or command line arguments) to specify the configuration values by prefixing the property name with **fabric8.**.

e.g. to deploy a maven project to a different profile name try:

    mvn fabric8:deploy -Dfabric8.profile=cheese -Dfabric8.version=1.1

By default the project artifacts are uploaded to the maven repository inside the fabric. If you wish to disable this and just update the profile configuration (e.g. if you're already pointing your fabric maven repository to your local maven repository), you can specify **fabric8.upload=false** as a property:

    mvn fabric8:deploy -Dfabric8.upload=false

## Adding additional configuration files into the profile

If you create the directory **src/main/fabric8** in your local project and add any configuration files or a ReadMe.md file (for documentation) in your project they will get automatically uploaded into the profile too.

e.g. in your project if you run this command:

    mkdir -p src/main/fabric8
    echo "## Hello World" >> src/main/fabric8/ReadMe.md
    mvn fabric8:deploy

Then when your profile will have a ReadMe.md wiki page uploaded.

## Specifying Properties

The following properties can be specified as elements inside the &lt;configuration&gt; section of the plugin in your pom.xml. e.g. the _profile_ configuration can be passed like this:

    <plugins>
      <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <configuration>
            <profile>${fabric8.profile}</profile>
          </configuration>
      </plugin>
    </plugins>

Or you can specify these properties using the command line or maven build properties - prefixing the property names with **fabric8.** for example to set the profile name, you could add this to your pom.xml..

    <project>
      ...
      <properties>
        <fabric8.profile>my-foo</fabric8.profile>
        ...
      </properties>
      ...

Or specify the command line:

    mvn fabric8:deploy -Dfabric8.profile=my-foo

### Property Reference

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>profile</td>
<td>The name of the fabric profile to deploy your project to. If not specified it defaults to the groupId-artifactId of your project.</td>
</tr>
<tr>
<td>serverId</td>
<td>The server ID used to lookup in <b>~/.m2/settings/xml</b> for the &lt;server&gt; element to find the username / password to login to the fabric. Defaults to <b>fabric8.upload.repo</b></td>
</tr>
<tr>
<td>jolokiaUrl</td>
<td>The Jolokia URL of the Fabric console. Defaults to <b>http://localhost:8181/jolokia</b></td>
</tr>
<tr>
<td>version</td>
<td>The fabric version in which to update the profile. If not specified it defaults to the current version of the fabric.</td>
</tr>
<tr>
<td>baseVersion</td>
<td>If the version does not exist, the baseVersion is used as the initial value of the newly created version. This is like creating a branch from the baseVersion for the new version branch in git.</td>
</tr>
<tr>
<td>parentProfiles</td>
<td>Space separated list of parent profile IDs to be added to the newly created profile. Defaults to <b>karaf</b>.</td>
</tr>
<tr>
<td>features</td>
<td>Space separated list of features to be added to the profile. e.g. a value could be this to include both the camel and cxf features: <code>&lt;features&gt;camel cxf&lt;/features&gt;</code></td>
</tr>
<tr>
<td>featureRepos</td>
<td>Space separated list of feature Repository URLs to be added to the Profile. Of the form <code>mvn:groupId/artifactId/version/xml/features</code></td>
</tr>
<tr>
<td>bundles</td>
<td>Space separated list of additional bundle URLs (of the form <code>mvn:groupId/artifactId/version</code> to be added to the newly created profile. Note you do not have to include the current maven project artifact; this configuration is intended as a way to list dependent required bundles.</td>
</tr>
<tr>
<td>upload</td>
<td>Whether or not the deploy goal should upload the local builds to the fabric maven repository. You could disable this step if you have configured your fabric maven repository to reuse your local maven repository. Defaults to true.</td>
</tr>
<tr>
<td>profileConfigDir</td>
<td>The folder in your maven project containing configuration files which should be deployed into the profile along with the artifact configuration. This defaults to <b>src/main/fabric8</b>. Create that directory and add any configuration files or documentation you wish to add to your profile.</td>
</tr>
</table>