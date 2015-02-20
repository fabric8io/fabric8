## Add a quickstart to the App Library

The web console has a Wiki which acts as a place you can store any documentation or [App Zips](appzip.html).

### Adding or removing Apps

You can add or remove an [App Zip](appzip.html) by dragging folders from the wiki's view to your file system; or dragging an [App Zip](appzip.html) from your file system and dropping it onto the file listing view in the Wiki.

### Using mvn fabric8:deploy

You can use the [mvn fabric8:deploy](mavenPlugin.html#deploying) goal to run the **zip** goal to make an [App Zip](appzip.html) and then post it to the App Library in your web console using the **FABRIC8_CONSOLE** environment variable.

e.g. run the following

    git clone https://github.com/fabric8io/quickstarts.git
    cd quickstarts
    mvn install
    cd quickstarts/java/camel-spring
    mvn clean install fabric8:deploy

* Now if you look at the [Library Tab in the web console](http://localhost:8585/hawtio/wiki/view) you should see a **java-camel-spring** folder with a nice camel icon on it.
* Click on the link, that should take you to the App page which should show the documentation
* On the top right you should see a **Run** button - click it to run the app
* Now you should be able to see any created kubernetes resources in the tabs: [Pods](http://localhost:8585/hawtio/kubernetes/pods), [Replication Controllers](http://localhost:8585/hawtio/kubernetes/replicationControllers) or [Services](http://localhost:8585/hawtio/kubernetes/services) in the web console. Note that the first thing to be created are the [replication controllers](replicationControllers.html) which then try to start the pods.
