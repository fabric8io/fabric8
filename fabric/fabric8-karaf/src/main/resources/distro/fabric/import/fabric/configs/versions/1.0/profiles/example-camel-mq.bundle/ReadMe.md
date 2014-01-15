# Example Camel MQ Bundle Demo

This example shows you how to integrate Camel routes with the A-MQ message broker.

There are two routes in this example:

* The first route generates a message every 5 seconds and sends it to the `camel-test` queue on the broker. The route uses the Camel ActiveMQ component to define a JMS producer endpoint, which can send messages to the broker.
* The second route pulls messages off the `camel-test` queue and sends them to the container's log.


### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. First of all, you need to deploy an A-MQ broker. Create a new child container and deploy the `mq-default` profile in a single step, by entering the following command at the console:

        fabric:container-create-child --profile mq-default root broker

1. Wait for the new child container, `broker`, to start up. Use the `fabric:container-list` command to check the status of the `broker` container and wait until the `[provision status]` is shown as `success`.
1. Now deploy the `example-camel-mq` profile. Create a new child container and deploy the `example-camel-mq` profile in a single step, by entering the following command at the console:

        fabric:container-create-child --profile example-camel-mq.bundle root camelmq

1. Log into the `camelmq` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect camelmq

1. View the container log using the `log:tail` command as follows:

        log:tail

 You should see some output like the following in the log:

        PLACEHOLDER

 To escape the log view, type Ctrl-C.

