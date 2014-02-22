# Example Camel Twitter

The example is demonstrating how to poll a constant feed of twitter searches and publish results in real time to a log.

There are one route in this example:

* The twitter-demo route polls for new search results from twitter, and outputs a log message with details about the tweet(s).

### How to run this example

You can deploy and run this example at the console command line, as follows:

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Create a new child container and deploy the `example-camel-twitter` profile in a single step, by entering the
 following command at the console:

        fabric:container-create-child --profile example-camel-twitter root mychild

1. Wait for the new child container, `mychild`, to start up. Use the `fabric:container-list` command to check the status of the `mychild` container and wait until the `[provision status]` is shown as `success`.
1. Log into the `mychild` container using the `fabric:container-connect` command, as follows:

        fabric:container-connect mychild

1. View the container log using the `log:tail` command as follows:

        log:tail

 You should see some output like the following in the log:

        2014-02-11 14:57:11,038 | INFO  | twitter://search | twitter-demo                     | rg.apache.camel.util.CamelLogger  176 | 97 - org.apache.camel.camel-core - 2.12.0.redhat-610337 | >>> Someone tweeted: Enjoy our one of a kind breakfast experience at Java Joe's!
        2014-02-11 14:57:11,040 | INFO  | twitter://search | twitter-demo                     | rg.apache.camel.util.CamelLogger  176 | 97 - org.apache.camel.camel-core - 2.12.0.redhat-610337 | >>> SomeoneElse tweeted: I need my Java coffee.

 To escape the log view, type Ctrl-C.
1. Disconnect from the child container by typing Ctrl-D at the console prompt.
1. Delete the child container by entering the following command at the console:

        fabric:container-delete mychild


### How to change the twitter search

You can configure which keyword(s) to use for the twitter search by editing the properties file `io.fabric8.examples.camel.twitter.properties` for the key `twitter.keywords`. By default the value is set to `camel`.

You can search for multiple keyword(s) using comma as or, and space as and. For example to search for camel or fuse, you can define the property as:

    twitter.search=camel,fuse

The search syntax is documented from twitter [here](https://support.twitter.com/articles/71577-using-advanced-search).
