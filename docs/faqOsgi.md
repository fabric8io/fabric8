### Questions On OSGi and Fabric8

#### How can I add my bundle and all its required bundles and features to fabric8?

The fabric8 approach to working with OSGi and fabric8 is to use the  [Fabric8 Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html) to deploy directly into a [profile](http://fabric8.io/gitbook/profiles.html) or to [create a profile zip](http://fabric8.io/gitbook/continuousDeployment.html#creating-profile-zips-via-maven) which can then be [imported](http://fabric8.io/gitbook/continuousDeployment.html#importing-profile-zips).

When using the [Fabric8 Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html) if you use the **bundle** packaging type in your maven pom.xml then the [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) will be used by default to add any dependent features or bundles to your profile.

#### Should I use features?

If you're using OSGi or [Apache Karaf](http://karaf.apache.org/) as your application server and you've already developed features XML files for grouping bundles together as a logical application; thats all cool and they work great in fabric8.

However there's not really a big need to develop them going forward; as its easier to just create a [profile zip file as part of your maven build](http://fabric8.io/gitbook/continuousDeployment.html#creating-profile-zips-via-maven) which can automatically include all your bundles for your project.

In addition the [OSGi Resolver](http://fabric8.io/gitbook/osgiResolver.html) provides a great way to automatically default the correct parent profiles, features and bundles to make it easier to get your OSGi bundles deploying correctly.

#### How do I install a pid.cfg file into a fabric?

In a stand alone [Apache Karaf](http://karaf.apache.org/) container you install configuration files (ending in .cfg) into the etc folder to configure things via OSGi Config Admin.

The fabric8 approach is similar; but rather than an etc folder on a specific containers file system, you install the file into a [profile](http://fabric8.io/gitbook/profiles.html). You can think of a profile as being a virtual etc folder which can be used by many containers.

The only difference is fabric8 uses the file name **pid.properties** rather than **pid.cfg**; so just rename the file and you should be good to go.

To add the file you can either:

* In the web console, go to the profile page you want to change, click the **Add** button (top right button bar) and create a new Properties file named **pid.properties** and then edit it to suit.
* Use the [Fabric8 Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html) to either deploy your profile or [create a profile zip](http://fabric8.io/gitbook/continuousDeployment.html#creating-profile-zips-via-maven) and then [import it](http://fabric8.io/gitbook/continuousDeployment.html#importing-profile-zips). You just need to create a file **src/main/fabric8/pid.properties** in your maven project.
* Perform a [git clone](http://fabric8.io/gitbook/git.html) and add the file into the profile's folder and git add / git commit / git push back the change;

