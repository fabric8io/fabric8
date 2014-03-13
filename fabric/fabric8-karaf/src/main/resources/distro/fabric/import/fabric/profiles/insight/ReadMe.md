## Insight

These profiles help provide additional insight into your running system.

* [kibana](/fabric/profiles/insight/kibana.profile) adds the [kibana](http://www.elasticsearch.org/overview/kibana/) web application into the Management Console for viewing and searching logs across all your containers.

This profile should be added to the root container running the Management Console. Once you've added it, reload the page and the Insight perspective should appear.

 ### Adding more insight into your containers

* [core](/fabric/profiles/insight/core.profile) the insight-core profile adds logging and metrics insight; which dumps logging and metric data into [ElasticSearch](http://www.elasticsearch.org/) so it can be queried by the kibana console.
* [camel](/fabric/profiles/insight/camel.profile) the insight-camel profile adds Camel message audit logging to [ElasticSearch](http://www.elasticsearch.org/). If you add this to any profiles you're deploying, you'll be able to query and view all your camel messages; and view reverse-engineered Gantt and sequence diagrams. To try this out try using the [loanbroker camel example profile](/fabric/profiles/example/camel/loanbroker.profile) and [insight-camel profile](/fabric/profiles/insight/camel.profile)
