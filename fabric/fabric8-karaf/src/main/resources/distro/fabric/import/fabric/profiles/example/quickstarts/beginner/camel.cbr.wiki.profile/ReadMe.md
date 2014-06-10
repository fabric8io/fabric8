# Camel Content Based Router (wiki) QuickStart

This quickstart is the wiki example of the Camel Content Based Router quickstart.

This example pickup incoming XML files, and depending on the content of the XML files, they are routed to different endpoints, as shown in figure below.

![Camel CBR diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-cbr-diagram.jpg)

The example comes with sample data, making it easy to try the example yourself.

### Building this example

This example comes as source code in the profile only. You can edit the source code from within the web console, by selecting the `cbr.xml` file in profile directoy listing, which opens the Camel editor, as shown in the figure below.

![Camel CBR editor](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-cbr-editor.jpg)

The editor alows you to edit the Camel routes, and have the containers automatic re-deploy when saving changes.

### How to run this example from web console

You can deploy and run this example from the web console, simply by clicking this button to create a new container

<div fabric-containers="containers" profile="{{profileId}}">
    <a class="btn" href="#/fabric/containers/createContainer?profileIds={{profileId}}"><i class="icon-plus"></i> Create a container for this profile</a>
</div>

... or follow the following steps.

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `example` --> `quickstarts` --> `beginner` --> `camel.cbr.wiki`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example from web console

This example comes with sample data which you can use to try this example.

You can send the sample data by follow the following steps.

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container. Click the *Camel* button in the navigation bar.
1. In the Camel tree, expand the `Endpoints` tree, and select the first node, which is `file://work/cbr/input`, and click the *Send* button in the sub navigation bar.
1. Click the *Choose* button and mark [x] for the five `data/order1.xml` ... `data/order5.xml` files.
1. Click the *Send 5 files* button in the top right corner
1. In the Camel tree, expand the `Routes` node, and select the first node, which is the `cbr-route` route. And click the *Diagram* button to see a visual representation of the route.
1. Notice the numbers in the diagram, which illustrate that 5 messages has been processed, of which 2 were from UK, 2 from US, and 1 others. 
1. You can click the *Log* button the navigation bar to see the business logging.


### Undeploy this example from web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner

