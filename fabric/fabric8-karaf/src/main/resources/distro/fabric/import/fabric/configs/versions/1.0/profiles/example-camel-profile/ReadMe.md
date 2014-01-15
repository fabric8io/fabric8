# Example Camel Hello

This is the _Hello World_ example for using Camel inside Fuse.

This profile executes some Camel routes which are stored directly in the Profile configuration; so it can be changed easily inside a Fabric without needing to perform a code release.

For example you can edit the Camel routes directly in the management console and perform rolling updates of the changes across containers using Fuse Fabric.

### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `example-camel-profile` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-camel-profile root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

 You should see some output like the following in the log:

        2013-10-16 12:03:47,403 | INFO  | #3 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 113 - org.apache.camel.camel-core - 2.12.0.redhat-610115 | >>> Hello from Fabric based Camel route! : 
        2013-10-16 12:03:52,403 | INFO  | #3 - timer://foo | fabric-client                    | rg.apache.camel.util.CamelLogger  176 | 113 - org.apache.camel.camel-core - 2.12.0.redhat-610115 | >>> Hello from Fabric based Camel route! :

 To escape the log view, type Ctrl-C.
1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Delete the child container by entering the following command at the console:

        fabric:container-delete mychild

