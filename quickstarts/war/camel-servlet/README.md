# Camel Servlet QuickStart

This example demonstrates how you can use Servlet to expose a http service in a Camel route, and run that in a servlet container such as Apache Tomcat.

The Camel route is illustrated in the figure below. The `servlet:hello` endpoint is listening for HTTP requests, and being routed using the Content Based Router. 

![Camel Servlet diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-servlet-diagram.jpg)

The request is being routed whether or not there is a HTTP query parameter with the name `name`. This is best illustrated as in the figure below, where we are running this quickstart. The first attempt there is no `name` parameter, and Camel returns a message that explains to the user, to add the parameter. In the send attempt we provide `?name=fabric` in the HTTP url, and Camel responses with a greeting message.

![Camel Servlet try](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/camel-servlet-try-quickstart.jpg)


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/war/camel-servlet` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/war/camel-servlet` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

The following information is divded into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-war-camel.servlet` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-war-camel.servlet root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.

### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `war` --> `camel-servet`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button


### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. 

1. Login the web console
1. Click the Runtime button in the navigation bar
1. Select the `mychild` container in the containers list, and click the *open* button right next to the container name.
1. A new window opens and connects to the container.
1. Click the *Tomcat* button, which lists all the WAR applications deployed. Click the url link for the `war-camel-servlet` application, which opens a web page, with further instructions how to try this example.
1. From the web console you can also click the *Camel* button to see various Camel information, such as from the Camel tree, expand the `Routes` node, and select the first node, which is the `helloRoute` route. And click the *Diagram* button to see a visual representation of the route (similar to the first figure shown in this readme file).
1. You can also click the *Log* button the navigation bar to see the business logging.


## Undeploy this example

To stop and undeploy the example in fabric8:

2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

