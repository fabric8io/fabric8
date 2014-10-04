## Developer workflow

If you tend to use an IDE for working with Java and things like [Apache Camel](http://camel.apache.org) and [Apache CXF](http://cxf.apache.org/) here's how to get started using your IDE or [Maven](http://maven.apache.org/) with [fabric8](http://fabric8.io/).

There's also a [more in depth screencast](http://www.christianposta.com/blog/?p=373) if you prefer to watch that first.

### To start create an archetype

We've got lots of [quickstarts](https://github.com/fabric8io/fabric8/tree/master/quickstarts) and [examples](https://github.com/fabric8io/fabric8/tree/master/tooling/examples) which we've converted into [Maven Archetypes](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html).

So if you have [installed Maven](http://maven.apache.org/download.cgi#Installation), type the following:

    mvn org.apache.maven.plugins:maven-archetype-plugin:2.2:generate -Dfilter=io.fabric8:

This will list all the various archetypes. Pick one that suits your fancy, e.g. **io.fabric8.archetypes:cbr-archetype** for the [Camel Content Based Router Quickstart](https://github.com/fabric8io/fabric8/tree/master/quickstarts/cbr).

Then enter these values:

    groupId:    cool
    artifactId: mydemo
    version:    1.0.0.SNAPSHOT
    package:    cool

And confirm with 'Y'. 

### Deploy your project to fabric8

Now type this in your shell:

    cd mydemo
    mvn fabric8:deploy
    
The [mvn fabric8:deploy](mavenPlugin.html) goal should build your project, upload your artefacts to [fabric's maven repository](mavenProxy.html) and create a new [Profile](profiles.html) for your maven project.

Open the [hawtio web console](http://hawt.io/) which by default is on [http://locahost:8181/](http://locahost:8181/) and navigate to the **Wiki** tab in the **Fabric** perspective you should see a **my** folder and inside that a **cbr** profile. 

Or just click on [this link to the my-cbr profile](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/my/cbr.profile) to go staight to the profile page.

Now create a new container for this profile; by clicking the **New** button on the top right of the profile page. Enter a container name and hit **Create and start container**

Your new container should now be running; you should be able to connect into the new JVM and see the camel route diagram with real time metrics etc.


### RAD Workflow

Now if you want to be able to change the code in an IDE and have it updated in fabric8, just run the following command in the fabric8 shell:

    Fabric8:karaf@root> fabric:watch *

This is like the Karaf command:

    Fabric8:karaf@root> dev:watch * 
  
which has been around for a long time and is quite awesome at watching a single container and detect snapshot builds of its bundles being rebuilt. 

The **fabric:watch** version is even more awesome than **dev:watch**; it will watch the local maven repository for all bundles which are snapshots which are provisioned in any profile in any container in the fabric (whatever machine they are running on, including remote machines, OpenShift/Docker/EC2 etc). If a local maven build is done on your machine, it’ll detect that, upload the new artefacts into fabric8’s maven repository and update any container running that profile in your fabric.

e.g. so just edit any code (e.g. the camel XML file) in your IDE; then just type:

    mvn install

Or or setup your IDE to run the "mvn install” goal whenever you save/compile the project.

Now every time you save in your IDE or do a maven build, fabric8 auto-updates all containers running that profile and you can see the effects of the update in real time via [hawtio web console](http://hawt.io/). Pretty neat!
