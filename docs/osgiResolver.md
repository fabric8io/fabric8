## OSGi Resolver

The OSGi resolver is used when you use the [Fabric8 Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html) with a maven project which uses the **bundle** packaging mechanism to make an OSGi bundle.

What it tries to do is automatically default the correct parent profiles, features and bundles to your profile for you; or to add any missing dependent bundles or features for you; so that your OSGi deployemnt is more likely to work first time.

You can try it out by using any of the [quickstarts](http://fabric8.io/gitbook/quickstarts.html) that ship with fabric8 and commenting out any **fabric8.** properties in the pom.xml file and running the following:

    mvn fabric8:deploy

Then click on the URL it outputs for the profile page and you should see the OSGi resolver do the right thing and pick all the right parent profiles, features and bundles for your project.

If you hit any problems with the resolver, please [let us know](https://github.com/fabric8io/fabric8/issues)!

### How it works

The maven plugin tries its best to pick the default parent profiles if you don't specify any. Based on the classes on your classpath it will choose profiles like **feature-camel** or **feature-cxf**; otherwise the default profile of **karaf** is used.

Once one or more parent profiles are picked; the current features and bundles are evaluated based on the current overlay profile configuration. Then the maven dependencies are analysed to see which bundles are required (i.e. bundles which are not test or provided scope); then all the available feature XML repositories are scanned for a feature which contains a version of that bundle; if so then that feature is used. If not the bundle is added to the profile.

### Working around issues

The OSGi Resolver will hopefully work well for you; particularly as it defaults to using all the curated features from the Apache projects like ActiveMQ, Camel, CXF, Felix, Karaf etc.

OSGi class loading is pretty complex stuff; so if you hit an issue with the OSGi Resovler adding too many dependencies or things being wrong you have a few options

* exclude the dependency from the resolver in your pom.xml by making the scope **provided**
* create your own features and add that to your pom.xml via the **fabric8.features** property
* add your own dependent bundles you wish to include in your pom.xml as a dependency

