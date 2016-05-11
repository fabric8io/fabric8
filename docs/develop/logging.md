## Logging

Logging is a key part of being able to understand microservices and diagnose issues. Consolidating log statements and events and being able to query and visualise them is an extremely useful tool.

When running the fabric8 microservices platform we recommend running the [Logging microservices](../logging.html) which runs [Elasticsearch](http://www.elasticsearch.com/products/elasticsearch/) for the back end storage and [Kibana](http://www.elasticsearch.com/products/kibana/) as the front end and fluentd as the collector.

As a microservice developer, you get _logging as a service_ for free with the [Logging microservices](../logging.html), though its recommended that you:

* write logs to standard output rather than to files on disk
* ideally use JSON output so thats its easy to automically parse it