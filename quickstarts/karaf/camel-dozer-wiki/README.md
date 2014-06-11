camel dozer wiki: demonstrates storing camel routes and dozer transforms in the wiki
===================================

## What is it?

This is the _Hello World_ example for using Camel where the Camel Routes are stored directly inside the Wiki along a Dozer transformation mapping file.

In addition the schemas folder in the profile is used to convert at runtime any XSDs into JAXB beans so they can be used efficiently inside Camel and in any Dozer transformations.

Since the Camel route is loaded from the wiki, it can be changed easily via the Management Console (with version history and the ability to revert changed) without needing to perform a code release.

For example you can edit the Camel routes directly in the Management Console and perform <a href="/fabric/profiles/docs/fabric/rollingUpgrade.md">rolling upgrades</a> of the changes across containers in a fabric.


## System requirements

Before building and running this quick start you need:

* Maven 3.0.4 or higher
* JDK 1.7
* Fabric8


## Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To build from the source code:

1. Change your working directory to `eip-dozer-wiki` directory.
1. Run `mvn clean install` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Change your working directory to `eip-dozer-wiki` directory.
1. Run `mvn fabric8:deploy` to upload the quickstart to the fabric container.

If you run the `fabric:deploy` command for the first then, it will ask you for the username and password to login the fabric container.
And then store this information in the local Maven settings file. You can find more details about this on the fabric8 website about the [Maven Plugin](http://fabric8.io/gitbook/mavenPlugin.html).


## How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `quickstarts-eip.dozer.wiki` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile quickstarts-eip.dozer.wiki root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail


### How to try this example

TODO: The example needs instructions how to try it


## Undeploy this example

To stop and undeploy the example in fabric8:

1. Disconnect from the child container by typing Ctrl-D at the console prompt.
2. Stop and delete the child container by entering the following command at the console:

        fabric:container-stop mychild
        fabric:container-delete mychild

