# Continuous Deployment

Having a single fabric be capable of managing all the [profiles](http://fabric8.io/gitbook/profiles.html) and containers within it using its own [git repository](http://fabric8.io/gitbook/profiles.html) for audit, version tracking and [rolling upgrades](http://fabric8.io/gitbook/rollingUpgrade.html) is very cool; but what if you want to manage more than one environment?

Typically you'll want a fabric for each environment as they are usually on different cycles in the continuous deployment pipeline. So how you do migrate code and profiles between fabrics (environments?)

## Moving Binaries

We tend to create various binaries; jar files, bundles, wars, ears, tarballs, zips as part of Java projects.

In development mode we expect you to use the [mvn fabric8:deploy goal in the maven plugin](http://fabric8.io/gitbook/mavenPlugin.html) to deploy a new maven project to a profile; then use the [fabric:watch * command line tool](http://fabric8.io/gitbook/developer.html) to provide rapid redeployment as you rebuild your binaries.

The _Continuous Deployment_ pipeline then starts when you release some binaries.

In fabric8 we expect released binaries to be stored in some kind of maven repository. That doesn't necessary mean you need a maven repository manager like [Nexus](http://www.sonatype.org/nexus/) or Artifactory; it could just be a block storage, NFS or HTTP based website thats rynch'd or backed up.

We then refer to those binaries in [profiles](http://fabric8.io/gitbook/profiles.html) by using maven coordinates.

So we assume you'd have an internal maven repository where all versions of released artifacts are stored.

## Moving profiles

One of the easiest ways to move profiles between environments is via _profile zips_. A profile zip is literally a zip file of one or more profile directories and their configuration files.

### Creating profile zips via maven

The [maven fabric8 plugin](http://fabric8.io/gitbook/mavenPlugin.html) supports the _fabric8:zip_ goal; which takes the same project metadata (such as [maven properties for the fabric8 plugin](http://fabric8.io/gitbook/mavenPlugin.html#property-reference).

So in any project type:

    mvn fabric8:zip

and a zip will be created and added to your local maven repository containing the profile (or possibly profiles in a multi-module maven project) as a zip file.

You can also add some XML to your pom.xml to ensure that a fabric8 profile zip is released with your project:

    <build>
      <plugins>
        <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <version>${fabric8-version}</version>
          <executions>
            <execution>
              <id>zip</id>
              <phase>package</phase>
              <goals>
                <goal>zip</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>

### Exporting profiles to zips

There's also a command line tool called _profile_export_ in the command line shell that lets you export all the profiles in a version; or all profiles matching a wildcard to a file.

    profile-export /tmp/myprofiles.zip

will output all profiles in the current version to the output file _/tmp/myprofiles.zip_. Or you could list a specific version

    profile-export -v 1.1 /tmp/myprofiles.zip

or use a wildcard

    profile-export -v 1.1 /tmp/myprofiles.zip quickstarts/*

### Using profile zips

The main way to use a profile zip right now is to install them in a fabric via the _profile-import_ command. This command takes a URL of one or more profile zips; usually using the maven coordinate URL.

e.g. to upload a single profile as a zip

    profile-import mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.1.0.CR1/zip/profile

or to upload all of the quickstarts:

    profile-import mvn:io.fabric8.quickstarts.fabric/fabric8-quickstarts-parent/1.1.0.CR1/zip/profile

You can specify a specific version to use:

    profile-import -v 1.1 mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.1.0.CR1/zip/profile

Or force a new version to be created before importing it:

    profile-import -n mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.1.0.CR1/zip/profile

Once the profiles are imported you should be able to use them from the command line; or you should be able to view them in the Wiki in the web console and create containers or migrate them etc.

