# Example MQ

This profile combines two messaging clients: a producer client and a consumer client. Effectively, it is a combination of the `example-mq-producer` profile and the `example-mq-consumer` profile.

This example deploys an ActiveMQ message producer, which sends a continuous stream of messages to the `FABRIC.DEMO` queue of the default broker.

For a complete ActiveMQ demonstration, you need a broker instance, which you can create by deploying the `mq-default` profile.

### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the requisite profiles in a single step, by entering the following command at the console:

        fabric:container-create-child --profile mq-default root mqchild

1. Wait for the new child container, `mqchild`, to start up. Use the `fabric:container-list` command to check the status of the `mqchild` container and wait until the `[provision status]` is shown as `success`.
1. Add the `example-mq` profile to the `mqchild` container, by entering the following command at the console:

        fabric:container-add-profile mqchild example-mq

1. Log into the `mqchild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mqchild

1. View the container log using the `log:tail` command as follows:

        log:tail

 You should see some output like the following in the log:

        2013-12-05 16:40:23 INFOorg.fusesource.mq.ConsumerThread Received test message: 259
        2013-12-05 16:40:23 INFOorg.fusesource.mq.ProducerThread Sent: test message: 259
        2013-12-05 16:40:24 INFOorg.fusesource.mq.ConsumerThread Received test message: 260
        2013-12-05 16:40:24 INFOorg.fusesource.mq.ProducerThread Sent: test message: 260

 To escape the log view, type Ctrl-C.
1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Delete the child container by entering the following command at the console:

        fabric:container-delete mqchild

