# Example Camel MQ

This example shows you how to integrate Camel routes with the A-MQ message broker using Fuse Fabric to discover the broker.

There are two routes in this example:

* The first route generates a message every 5 seconds and sends it to the `camel-test` queue on the broker. The route uses the Camel ActiveMQ component to define a JMS producer endpoint, which can send messages to the broker.
* The second route pulls messages off the `camel-test` queue and sends them to the container's log.


### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. First of all, you need to deploy an A-MQ broker. Deploy the `mq-default` profile to the `root` container, by entering the following command at the console:

        fabric:container-add-profile root mq-default

1. Now deploy the `example-camel-mq` profile to the `root` container, by entering the following command at the console:

        fabric:container-add-profile root example-camel-mq

1. View the container log using the `log:tail` command as follows:

        log:tail

 You should see some output like the following in the log:

        2013-11-29 13:53:10,528 | INFO  | umer[camel-test] | fabric                           | ?                                   ? | 286 - org.apache.camel.camel-core - 2.12.0.redhat-610181 | Exchange[Body: Fabric Camel Example: 01:11:10.522)]
        2013-11-29 13:53:15,529 | INFO  | umer[camel-test] | fabric                           | ?                                   ? | 286 - org.apache.camel.camel-core - 2.12.0.redhat-610181 | Exchange[Body: Fabric Camel Example: 01:11:15.524)]

 To escape the log view, type Ctrl-C.

