## Fabric8 Maven Plugin

This maven plugin makes it easy to create or update a fabric profile from your maven project.

When you deploy your project to a fabric profile with this plugin the following takes place:

* uploads any artifacts into the fabric's maven repository
* lazily creates the fabric profile or version you specify
* adds/updates the maven project artifact into the profile configuration
* adds any additional parent profile, bundles or features to the profile.

### Configuring the plugin

First you will need to edit your **~/.m2/settings.xml** file to add the fabric server's user and password so that the maven plugin can login to the fabric..

e.g. add this to the &lt;servers&gt; element:

    <server>
      <id>fabric8.upload.repo</id>
      <username>admin</username>
      <password>admin</password>
    </server>

If you don't do this, the first time you use the fabric8 plugin it will ask you if you wish to update your ~/.m2/settings.xml file and prompt you for the information; then update it.

The default fabric upload maven repo ID is **fabric8.upload.repo**. You can define as many &lt;server&gt; elements in your settings file as you like for each of the fabrics you wish to work with. Then to pick the credentials to use for a server specify the server id as the **serverId** property on the fabric8 maven plugin configuration section (see below) or use the **fabric8.serverId** maven property.

### Using the plugin

To use the fabric8 maven plugin to deploy into a fabric profile on any maven project just type:

    mvn io.fabric8:fabric8-maven-plugin:1.2.0.Beta4:deploy

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
    mvn io.fabric8:fabric8-maven-plugin:1.2.0.Beta4:deploy

Then you should see this profile being created at the [my-rest/rest profile page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/my/rest.profile) which should have a bundle and some features added too (click on the Bundle and Feature tabs and you should see those).

### Configuring the fabric server

By default the fabric8 maven plugin deploys to a local fabric server using the url

    http://localhost:8181/jolokia
    
To use a remote fabric server you can either configure this in the plugin in the **pom.xml** file using the **jolokiaUrl** configuration as shown below

    <plugins>
      <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <configuration>
            <jolokiaUrl>http://someServer:8181/jolokia</jolokiaUrl>
          </configuration>
      </plugin>
    </plugins>

... or specify the url in the command line

    mvn fabric8:deploy -Dfabric8.jolokiaUrl=http://someServer:8181/jolokia

#### Quick deploy without testing

Sometimes you may want to skip testing before deploying, if you have done a trivial change. This can be done by speciftying ```-DskipTests``` via the command line as shown:

    mvn fabric8:deploy -DskipTests

### Specifying the profile information in the plugin configuration

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

At any point in your tree of maven projects you can define a maven **fabric8.profile** property to specify exactly where it gets deployed; along with any other property on the plugin (see the Property Reference below).

### Specifying features, additional bundles, repositories and parent profiles

As of 1.1.0.CR6 we have the [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) which if you are deploying a **bundle** packaging project it will try to automatically choose the best parent profiles, features and bundles for your bundle.

You can also explicitly specify additional configuration in the maven plugin using maven properties...

    <properties>
        <!-- fabric8 deploy profile configuration -->
        <fabric8.profile>quickstarts-karaf-cxf-rest</fabric8.profile>
        <fabric8.parentProfiles>feature-cxf</fabric8.parentProfiles>
        <fabric8.features>cxf-jaxrs swagger</fabric8.features>
    </properties>

Notice we can pass in a space-separated list of features to include in the profile.

We've used space separated lists for the parent profile IDs, features, repositories and bundles so that its easy to reuse maven properties for these values (for example to add some extra features in a child maven project while inheriting from the parent project).

The [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) will add any missing dependencies via analysing your projects dependency tree (ignoring all test and provided scope dependencies).

So to force the OSGi Resovler to ignore a particular dependency in your pom.xml, just mark it as scope provided.

If you wish you can disable the OSGi Resolver completely just specify the **fabric8.useResolver** property as being **false**.

#### Specifying configuration using maven properties on the command line

You can also use maven property values (or command line arguments) to specify the configuration values by prefixing the property name with **fabric8.**.

e.g. to deploy a maven project to a different profile name try:

    mvn fabric8:deploy -Dfabric8.profile=cheese -Dfabric8.profileVersion=1.1

By default the project artifacts are uploaded to the maven repository inside the fabric. If you wish to disable this and just update the profile configuration (e.g. if you're already pointing your fabric maven repository to your local maven repository), you can specify **fabric8.upload=false** as a property:

    mvn fabric8:deploy -Dfabric8.upload=false

### Adding additional configuration files into the profile

If you create the directory **src/main/fabric8** in your local project and add any configuration files or a ReadMe.md file (for documentation) in your project they will get automatically uploaded into the profile too.

e.g. in your project if you run this command:

    mkdir -p src/main/fabric8
    echo "## Hello World" >> src/main/fabric8/ReadMe.md
    mvn fabric8:deploy

Then when your profile will have a ReadMe.md wiki page uploaded.

### Adding PID files into the profile

Probably one of the most interesting use cases of the `src/main/fabric8` directory, is uploading PID files into your
profile. For example you can take `my.pid.properties` file with the following contents:

    foo=bar
    baz=qux

...and place it in the `src/main/fabric8` directory. After executing `mvn fabric8:deploy` command, the
`my.pid.properties` file will be uploaded into your profile as a `my.pid` PID configuration:

    > profile-display myprofile
    ...
    Configuration details
    ----------------------------
    PID: my.pid
      foo bar
      baz qux

#### Using Maven placeholders in PID files

We recommend to add the `src/main/fabric8` directory to the list of the resources
[filtered](http://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html) by Maven. For example:

    <build>
      <resources>
        <resource>
          <directory>src/main/fabric8</directory>
          <filtering>true</filtering>
        </resource>
      </resources>
      <plugins>
        <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <version>${fabric8.version}</version>
          <configuration>
            <profile>invoicing</profile>
          </configuration>
        </plugin>
      </plugins>
    </build>

With Maven resources filtering enabled, you can use placeholder in your PID files:

    jarUrl = mvn:${project.groupId}:${project.artifactId}:${project.version}

Keep in mind that even if Maven resource filtering is not enabled, `${project.groupId}`, `${project.artifactId}` and 
`${project.version}` placeholders will be still expanded into the project version of the current Maven module.


#### Specifying the minimum number of required containers for the profile

You can specify the minimum number of instances of a profile that are expected via the **fabric8.minInstanceCount** property. This value defaults to **1** so that it means the profile you deploy should be instantiated. See the [requirements documentation](requirements.html) for more details.

What this means is that out of the box if you deploy a profile then view the Profiles tab in the Runtime section of the console, you should see a warning if the profile is not running yet. If you then click on the red button for the missing profile it takes you straight to the _Create Container_ page for the  profile. This means you don't have to go hunting around the wiki for the profile to create.

Also if you deploy the **autoscale** profile then this will automatically create new containers if their requirement count increases.


### Specifying Properties

Our recommendation is to use maven properties to configure the fabric8 maven plugin as follows; you just need to add the **fabric8.** prefix to any property name. This is then easier to work with across multi-maven projects and its easier to inherit values etc.

    <project>
      ...
      <properties>
        <fabric8.profile>my-foo</fabric8.profile>
        ...
      </properties>
      ...

Which is equivalent to specifying them on the command line:

    mvn fabric8:deploy -Dfabric8.profile=my-foo

If you really want to you can specify the properties (without the **fabric8.** prefix) in the plugin configuration:

    <plugins>
      <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <configuration>
            <profile>${fabric8.profile}</profile>
          </configuration>
      </plugin>
    </plugins>


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
<td>abstractProfile</td>
<td>Whether the profile is marked as abstract. Defaults to <b>false</b></td>
</tr>
<tr>
<td>serverId</td>
<td>The server ID used to lookup in <b>~/.m2/settings/xml</b> for the &lt;server&gt; element to find the username / password to login to the fabric. Defaults to <b>fabric8.upload.repo</b></td>
</tr>
<tr>
<td>jolokiaUrl</td>
<td>The Jolokia URL of the Fabric console. Defaults to <b>http://localhost:8181/jolokia</b>. Username and password can also be specified in the jolokiaUrl which allows to use a custom credentials, or in cases where storing login information in the Maven <tt>settings.xml</tt> file is not desired. See further below for more details.</td>
</tr>
<tr>
<td>profileVersion</td>
<td>The profile version in which to update the profile. If not specified it defaults to the current version of the fabric.</td>
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
<td>minInstanceCount</td>
<td>The minimum required number of instances of this profile. This defaults to 1 if you do not specify it. See the [requirements documentation](requirements.html) for more details.</td>
</tr>
<tr>
<td>upload</td>
<td>Whether or not the deploy goal should upload the local builds to the fabric maven repository. You could disable this step if you have configured your fabric maven repository to reuse your local maven repository. Defaults to true.</td>
</tr>
<tr>
<td>ignoreProject</td>
<td>If set to true then this maven project is ignored when goals like fabric8:deploy are invoked from a parent project. Defaults to false.</td>
</tr>
<tr>
<td>includeArtifact</td>
<td>If set to false then the maven artifact of the project is ignored; its not uploaded and not added/updated in the profile. Defaults to true.</td>
</tr>
<tr>
<td>artifactBundleType</td>
<td>Overrides the type (file extension) of the project artifact bundle to include in the profile (see includeArtifact).
Normally the type is set to the project POM's packaging value.</td>
</tr>
<tr>
<td>artifactBundleClassifier</td>
<td>Overrides the classifier of the project artifact bundle to include in the profile (see includeArtifact).
Normally the classifier is empty. The artifactBundleType parameter must always be specified explicitly
when using the artifactBundleClassifier parameter.</td>
</tr>
<tr>
<td>omitDependenciesFromRequirements</td>
<td>Depending on the type of project that is being built, fabric8 will include some of the project's dependencies as profile requirements. 
Depending on the container used, the requirements might indicate dependencies to be downloaded by fabric8 during provisioning. 
This property can be set to true if adding dependencies as requirements is not appropriate for your project's profile. 
By default this property is set to false.</td>
</tr>
<tr>
<td>profileConfigDir</td>
<td>The folder in your maven project containing configuration files which should be deployed into the profile along with the artifact configuration. This defaults to <b>src/main/fabric8</b>. Create that directory and add any configuration files or documentation you wish to add to your profile.</td>
</tr>
<tr>
<td>includeReadMe</td>
<td>Whether or not to include the project readme file (if exists). Notice that if there is already a readme file in <b>profileConfigDir</b>, then that file is included, and <b>not</b> the project readme file. Defaults to true.</td>
</tr>
<tr>
<td>webContextPath</td>
<td>For web applications (projects with *war* packaging) this property specifies the context path to use for the web application; such as "/" or "/myapp". This value defaults to the archetype id.</td>
</tr>
<tr>
<td>replaceReadmeLinksPrefix</td>
<td>Used by the `zip` goal, which allows to prefix any links in the `readme.md` files to make the links work in both github and as links in the fabric wiki. For example the fabric quickstarts uses this, by prefixing with `/fabric/profiles/quickstarts/`.</td>
</tr>
<tr>
<td>useResolver</td>
<td>Whether or not the OSGi resolver is used for bundles or karaf based containers to deduce the additional bundles or features that need to be added to your projects dependencies to be able to satisfy the OSGi package imports. Defaults to false.</td>
</tr>
<tr>
<td>locked</td>
<td>Whether or not the created profile should be locked (so its read only). Defaults to false.</td>
</tr>
</table>

### Specifying credentials from command line

The **fabric8:deploy** goal will by default read the username and password from the local Maven `settings.xml` file. This may not be desired to store password as plain-text. The option `jolokiaUrl` can be used to specify the url for the remote fabric server including username and password. For example to use username `scott` and password `tiger` then type:

     fabric8:deploy -Dfabric8.jolokiaUrl=http://scott:tiger@localhost:8181/jolokia

Tip: You can get the jolokia url using `fabric:info` from the fabric shell.

### Generating Karaf shell scripts for each profile

You can also use the **fabric8:script** goal using the same configuration above to auto-generate a profile create karaf script. The script can then be used from the Karaf shell to create the profile. 

From the Karaf shell you can use the `shell:source` command to run the script, eg:

    shell:source file:somepath\target\profile.karaf

The **fabric:script** creates the script file by default as `target\profile.karaf`. The name of the output file can be configured using the following options:

### Property Reference

<table class="table table-striped">
<tr>
<th>Parameter</th>
<th>Description</th>
</tr>
<tr>
<td>outputFile</td>
<td>The name of the script file. Is by default <tt>target\profile.karaf</tt></td>
</tr>
<tr>

