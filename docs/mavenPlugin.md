# Fabric8 Maven Plugin

This maven plugin makes it easy to create or update a fabric profile from your maven project.

When you deploy your project to a fabric profile with this plugin the following takes place:

* uploads any artifacts into the fabric's maven repository
* lazily creates the fabric profile or version you specify
* adds/updates the maven project artifact into the profile configuration

## Configuring the plugin

First you will need to edit your **~/.m2/settings.xml** file to add the fabric server's user and password so that the maven plugin can login to the fabric..

e.g. add this to the &lt;servers&gt; element:

    <server>
      <id>fabric8.upload.repo</id>
      <username>admin</username>
      <password>admin</password>
    </server>

The default fabric upload maven repo ID is **fabric8.upload.repo**. You can define as many as you like (for your different credentials). Then to pick the credentials to use for a server specify the server id as the **fabricServerId** property on the fabric8 maven plugin configuration section (see below).

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

The project will then be built and deployed into a profile in the fabric. By default the profile is named $group-$artifact but you can override the profile ID, version ID and location of the Fabric Jolokia URL via the maven plugin configuration or via the command line.

e.g. to try it out

    cd quickstarts/rest
    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

Then you should see this profile being created at the [org.jboss.quickstarts.fuse/rest profile page](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/org.jboss.quickstarts.fuse/rest.profile) which should have a bundle added too (click on the Bundle tab and you should see the bundle).

## Customizing the behaviour via the command line arguments

To use a different version or profile, just specify them on the command line.

e.g. to deploy a maven project to a different profile name try:

    mvn fabric8:deploy -Dprofile=cheese -Dversion=1.1

By default the project artifacts are uploaded to the maven repository inside the fabric. If you wish to disable this and just update the profile configuration (e.g. if you're already pointing your fabric maven repository to your local maven repository), you can specify **upload=false** as a property:

    mvn fabric8:deploy -Dupload=false

## Adding additional configuration files into the profile

If you create the directory **src/main/fabric8** in your local project and add any configuration files or a ReadMe.md file (for documentation) in your project they will get automatically uploaded into the profile too.

e.g. in your project if you run this command:

    mkdir -p src/main/fabric8
    echo "## Hello World" >> src/main/fabric8/ReadMe.md
    mvn fabric8:deploy

Then when your profile will have a ReadMe.md wiki page uploaded.

## Properties

The following properties can be specified on the command line or as configuration parameters in the plugin configuration in your pom.xml:

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
<td>version</td>
<td>The fabric version in which to update the profile. If not specified it defaults to the current version of the fabric.</td>
</tr>
<tr>
<td>fabricServerId</td>
<td>The server ID used to lookup in <b>~/.m2/settings/xml</b> for the &lt;server&gt; element to find the username / password to login to the fabric. Defaults to <b>fabric8.upload.repo</b></td>
</tr>
<tr>
<td>jolokiaUrl</td>
<td>The Jolokia URL of the Fabric console. Defaults to <b>http://localhost:8181/jolokia</b></td>
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