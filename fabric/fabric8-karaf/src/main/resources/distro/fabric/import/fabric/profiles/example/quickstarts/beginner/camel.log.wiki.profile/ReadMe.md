# Camel Log (wiki) QuickStart

This quickstart is the wiki example of the Camel Log quickstart.

This is a simple Apache Camel application that logs a message to the server log every 5th second.

This example is implemented using solely the XML DSL (there is no Java code). The source code is provided in the following XML file `src/main/resources/OSGI-INF/blueprint/camel-log.xml`, which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/beginner/camel-log/src/main/resources/OSGI-INF/blueprint/camel-log.xml).

This example uses a timer to trigger every 5th second, and then writes a message to the server log.

This example comes as source code in the profile only. You can edit the source code from within the web console, by selecting the `camel-log.xml` file in profile directoy listing, which opens the Camel editor, as shown in the figure below.

![Camel Log editor](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-log-editor.png)

The editor alows you to edit the Camel routes, and have the containers automatic re-deploy when saving changes.


### How to run this example 

You can deploy and run this example from the web console, simply by clicking this button to create a new container

<div fabric-containers="containers" profile="{{profileId}}">
    <a class="btn" href="#/fabric/containers/createContainer?profileIds={{profileId}}"><i class="icon-plus"></i> Create a container for this profile</a>
</div>

... or follow the following steps.

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `example` --> `quickstarts` --> `beginner` --> `camel.log.wiki`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example using the web console

This example comes with sample data which you can use to try this example

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Log* button in the navigation bar.
1. You can also click the *Camel* button in the top navigation bar, to see information about the Camel application. For example in the Camel tree, select the Camel route `log-example-context`, and click the *Diagram* button in the sub navigation bar, to see a visual representation of the Camel route.


## Undeploy this example using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner

