## Continuous Deployment

Having a single fabric be capable of managing all the [profiles](profiles.html) and containers within it using its own [git repository](git.html) for audit, version tracking and [rolling upgrades](rollingUpgrade.html) is very cool; but what if you want to manage more than one environment?

Typically you'll want a fabric for each environment as they are usually on different cycles in the continuous deployment pipeline. So how you do migrate code and profiles between fabrics (environments?)

### Moving Binaries

We tend to create various binaries; jar files, bundles, wars, ears, tarballs, zips as part of Java projects.

In development mode when working with _SNAPSHOT_ versions we expect you to use the [mvn fabric8:deploy goal in the maven plugin](mavenPlugin.html) to deploy a new maven project to a profile; then use the [fabric:watch * command line tool](developer.html) to provide rapid redeployment as you rebuild your binaries; then as you rebuild on your local machine your code gets auto-redeployed on your fabric containers.

The _Continuous Deployment_ pipeline then starts when you release some binaries. In fabric8 we expect released binaries to be stored in some kind of maven repository. That doesn't necessary mean you need a maven repository manager like [Nexus](http://www.sonatype.org/nexus/) or Artifactory; it could just be a block storage, NFS or HTTP based website thats rynch'd or backed up.

We then refer to those binaries in [profiles](profiles.html) by using maven coordinates.

So we assume you'd have an internal maven repository where all versions of released artifacts are stored.

### Moving profiles

Since each environment has its own git repository you could try using code reviews, pull requests and cherry picking to move changes in one git repository to another. There is a ton of tooling out there (e.g. [gerrit](https://code.google.com/p/gerrit/)) for doing this kind of thing.

One of the easiest ways to move profiles between environments is via _profile zips_. A profile zip is literally a zip file of one or more profile directories and their configuration files which then gets released during the normal software release cycle; so each team is responsible for its own profiles (the code used inside them and its configurations).

#### Creating profile zips via maven

The [maven fabric8 plugin](mavenPlugin.html) supports the _fabric8:zip_ goal; which takes the same project metadata (such as [maven properties for the fabric8 plugin](mavenPlugin.html#property-reference).

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

#### Exporting profiles to zips

There's also a command line tool called _profile_export_ in the command line shell that lets you export all the profiles in a version; or all profiles matching a wildcard to a file.

    profile-export /tmp/myprofiles.zip

will output all profiles in the current version to the output file _/tmp/myprofiles.zip_. Or you could list a specific version

    profile-export -v 1.1 /tmp/myprofiles.zip

or use a wildcard

    profile-export -v 1.1 /tmp/myprofiles.zip quickstarts/*

#### Importing profile zips

The main way to use a profile zip right now is to install them in a fabric via the _profile-import_ command. This command takes a URL of one or more profile zips; usually using the maven coordinate URL.

e.g. to upload a single profile as a zip

    profile-import mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.2.0.Beta4/zip/profile

or to upload all of the quickstarts:

    profile-import mvn:io.fabric8.quickstarts.fabric/fabric8-quickstarts-parent/1.2.0.Beta4/zip/profile

You can specify a specific version to use:

    profile-import -v 1.1 mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.2.0.Beta4/zip/profile

Or if you want to import to a new version, you would need to create the new version first using `create-version`

    create-version 1.2

and then import to new version with

    profile-import -v 1.2 mvn:io.fabric8.quickstarts.fabric/camel-cdi/1.2.0.Beta4/zip/profile

Once the profiles are imported you should be able to use them from the command line; or you should be able to view them in the Wiki in the web console and create containers or migrate them etc.

#### Combining profile zips into a new branch in a fabric's git repo

Another approach to moving profile zips from one environment to another is to use a maven build to aggregate a number of different profile zip files (usually released by different teams on different schedules) together in a _Continuous Integration_ build to make a new branch in a git repository for an environment.

For example you could have a testing environment and a build for testing which takes a different permutation of versions of profile zips and consolidates them together into a new version (branch) in the git repository of the testing environment.

If the tests pass, the version can move through to production etc.

To do this you can use the  [maven fabric8 plugin](mavenPlugin.html) _fabric8:branch_ goal as follows:

The following section of a pom.xml will create a new branch, unzip the dependent profile zips, add them to git and commit and push into the testing environment git:

    <dependencies>
      <!-- lets take the base profiles from fabric8 -->
      <dependency>
        <groupId>io.fabric8</groupId>
        <artifactId>fabric8-profiles</artifactId>
        <version>${fabric8-version}</version>
        <type>zip</type>
      </dependency>

      <!-- in this example lets use the quickstart profiles but these could be any profile zips -->
      <dependency>
        <groupId>org.jboss.quickstarts.fuse</groupId>
        <artifactId>jboss-quickstarts-fuse-parent</artifactId>
        <version>${fabric8-version}</version>
        <type>zip</type>
        <classifier>profile</classifier>
      </dependency>
    </dependencies>

    <build>
      <plugins>
        <plugin>
          <groupId>io.fabric8</groupId>
          <artifactId>fabric8-maven-plugin</artifactId>
          <version>${fabric8-version}</version>
          <executions>
            <execution>
              <id>branch</id>
              <phase>compile</phase>
              <goals>
                <goal>branch</goal>
              </goals>
              <configuration>
                <!-- lets choose the name of the version in the git repository where we will create the branch -->
                <branchName>${environment-version</branchName>
              </configuration>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>

#### Figuring out the best approach

Different teams and companies have different policies for moving software through the Continuous Deployment pipeline; so try find the approach that suits your team, its workflow and the tools and processes you are using.

We'd love to [hear from you](http://fabric8.io/community/index.html) how you get on using fabric8 in your continuous deployment pipeline. If you have any suggestions for how we can improve fabric8 to better support your continuous deployment pipeline please [raise an issue](https://github.com/fabric8io/fabric8/issues) we love feedback!

### Automating the creation of a fabric

Many customers want an easy, repeatable way to spin up a fabric and all the various containers they need in an automated way. This is particularly useful as part of a _Continuous Deployment_ build process for performing integration, load & soak testing.

To do this fabric8 has an [Auto Scaler](http://fabric8.io/gitbook/requirements.html) which allows you to define how many instances of each profile you need and the auto scaler will automatically create the containers you need; using the available resources and automatically create new containers if there is a hardware or software failure.
