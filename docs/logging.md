## Logging

Logging is a key part of being able to maintain and understand distributed systems. Consolidating log statements and events and being able to query and visualise them is an extremely useful tool.

With fabric8 we recommend using:

* [Elasticsearch](http://www.elasticsearch.com/products/elasticsearch/) as the back end storage of logs and events in your system as its easy to scale, provides replicated data for resilience and is very easy to query using complex structural and textual queries
* [Kibana](http://www.elasticsearch.com/products/kibana/) as the console to view, query and analyze the logs
* to collect the logs we tend to use this [logspout fork](https://github.com/jimmidyson/logspout) which is a small docker container for collecting logs which appends the docker and kuberentes metadata. Or you can use [LogStash](http://www.elasticsearch.com/products/logstash/) directly inside a container if you wish

### How to use Logging in fabric8

If you are running Fabric8 with the [Fabric8 Console](console.html) then go to the **Apps** tab.

* click the **Run...** button and select the **Elasticsearch** app to and run it.
* click the **Run...** button and select the **Kibana** app to and run it.

You will also need to run the logspout container on each node to ensure the logs are collected. This is done automatically if you [use the bash script to install fabric8](openShiftDocker.html) with the **-k** command line option enabled. Hopefully one day we'll be able to make logspout an [app](apps.html) you can easily run in the same way.

Once the above is running, the [Fabric8 Console](console.html) should have a **Logs** tab letting you view and search the logs via [Kibana](http://www.elasticsearch.com/products/kibana/). When you view the **Pods** tab you should be able to select one or more [pods](pods.html) and click on the **Logs** button to view the logs for the selected pods.