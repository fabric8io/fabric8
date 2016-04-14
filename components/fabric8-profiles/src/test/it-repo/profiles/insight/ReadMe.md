## Insight

These profiles help provide additional insight into your running system.

* [console](/fabric/profiles/insight/console.profile) adds the [kibana](http://www.elasticsearch.org/overview/kibana/) and [eshead](http://mobz.github.io/elasticsearch-head/) web application into the web console for viewing and searching logs across all your containers.

This profile should be added to the root container running the web console. Once you've added it, reload the page and the Insight perspective should appear.

 ### Adding more insight into your containers

* [core](/fabric/profiles/insight/core.profile) the insight-core profile adds logging insight; which dumps logging data into [ElasticSearch](http://www.elasticsearch.org/) so it can be queried by the kibana console.
* [metrics.elasticsearch](/fabric/profiles/insight/metrics.elasticsearch.profile) the metrics-elasticsearch profile adds metrics insight and stores data into [ElasticSearch](http://www.elasticsearch.org/).
* [camel](/fabric/profiles/insight/camel.profile) the insight-camel profile adds Camel message audit logging to [ElasticSearch](http://www.elasticsearch.org/). If you add this to any profiles you're deploying, you'll be able to query and view all your camel messages; and view reverse-engineered Gantt and sequence diagrams. To try this out try using the [loanbroker camel example profile](/fabric/profiles/example/camel/loanbroker.profile) and [insight-camel profile](/fabric/profiles/insight/camel.profile)
* [jetty](/fabric/profiles/insight/jetty.profile) the insight-jetty profile adds an HTTP request logs for Jetty in [ElasticSearch](http://www.elasticsearch.org/).

### Configuration

Collected data are stored into Elasticsearch indices, one index per day.
Management of those indices can be tuned in the core profile [configuration](/fabric/profiles/insight/core.profile/io.fabric8.insight.elasticsearch-default.properties).
