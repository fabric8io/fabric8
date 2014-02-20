# Fabric8 Maven Plugin

This maven plugin makes it easy to lazily create a profile in a fabric and add/update it to use the artifact from a maven build.

## Configuring the plugin

First you will need to edit your **~/.m2/settings.xml** file to add the fabric server's user and password.

e.g. add this to the &lt;servers&gt; element:

    <server>
      <id>fabric8.upload.repo</id>
      <username>admin</username>
      <password>admin</password>
    </server>

The default fabric upload maven repo ID is **fabric8.upload.repo**. You can define as many as you like (for your different credentials). Then to pick the credentials to use for a server specify the server id as the **fabricServerId** property on the fabric8 maven plugin configuration section.

## Using the plugin

Now to use the maven plugin to deploy any maven project into a fabric8 profile try this:

    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

If you have added the fabric8 maven plugin to your pom.xml as follows:

    <plugins>
      <plugin>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-maven-plugin</artifactId>
      </plugin>
    </plugins>

Then you can use the more concise maven goal:

    mvn fabric8:deploy

The project will then be built and deployed into a profile in the fabric. By default the profile is named $group-$artifact but you can override the profile ID, version ID and location of the Fabric Jolokia URL via the maven plugin configuration or via the command line.

e.g. to try it out

    cd quickstarts/rest
    mvn io.fabric8:fabric8-maven-plugin:1.0.0-SNAPSHOT:deploy

Then you should see this profile being created: http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/org.jboss.quickstarts.fuse/rest.profile
Which should have a bundle added too.

## Customizing the behaviour via the command line arguments

To use a different version or profile, just specify them on the command line.

e.g. to deploy a maven project to a different profile name try:

    mvn fabric8:deploy -Dprofile=cheese -Dversion=1.1

By default the project artifacts are uploaded to the maven repository inside the fabric. If you wish to disable this and just update the profile configuration (e.g. if you're already pointing your fabric maven repository to your local maven repository), you can specify **upload=false** as a property:

    mvn fabric8:deploy -Dupload=false
