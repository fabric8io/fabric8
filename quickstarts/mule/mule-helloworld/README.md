# Mule HelloWorld QuickStart

This example shows how to start a simple web server on port 2121 in a MuleESB container.

So if you started the container on the localhost, you would point your browser to http://localhost:2121.

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/mule/mule-helloworld` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `quickstarts/mule/mule-helloworld` directory.
1. Run `mvn install fabric8:deploy` to upload the quickstart to the fabric container.

If you are running the `fabric:deploy` command for the first time then, it will ask you for the username and password to login the fabric container.
It will then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

The following information is divided into three sections, whether you are using the command line shell in fabric, or using the web console, or run the example from the source code.

### Using the command line shell

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-mule-helloworld` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-mule-helloworld root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.


### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `mule` --> `helloworld`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button

### From the source code

Follow the instructions from the _Building this example_ section, and after you have built the source code run the following command:

1. Run `mvn exec:java` to run the example as a standalone Mule application.

Running outside fabric means that you do not have the fabric web console or fabric server to manage the application. You may want to use `mvn exec:java` during development and to quickly try your code changes.


## How to try this example

To see this example work point your browser to the ip of your container and port 2121.

So if you started the container on the localhost, you would point your browser to http://localhost:2121.

You should see a basic page with the heading 'It Works!'.


## Undeploy this example

The following information is divided into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the command line shell

To stop and undeploy the example in fabric8:

1. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner
