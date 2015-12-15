## Metrics

Capturing historical metrics an extremely useful tool to help diagnose your running system.

With fabric8 we recommend using:

* [InfluxDB](http://influxdb.com/) or [RHQ Metrics](https://github.com/rhq-project/rhq-metrics) as the back end storage of historical metric data as they are both easy to scale, provide replicated data for resilience and are very easy to query
* [Grafana](http://grafana.org/) as the console to view, query and analyze metrics
* to collect the metrics we use [cAdvisor](https://github.com/google/cadvisor) for collecting metrics from docker containers and [jAdvisor](https://github.com/jimmidyson/jadvisor) for collecting metrics from JMX using [jolokia](http://jolokia.org/)


### How to use Metrics in fabric8

If you are running Fabric8 with the [Fabric8 Console](console.html) then go to the **Apps** tab.

* click the **Run...** button and select the **InfluxDB** app to and run it.
* click the **Run...** button and select the **Grafana** app to and run it.

You will also need to run the cadvisor/jadvisor containers on each node to ensure the metrics are collected. This is done automatically if you [use the bash script to install fabric8](getStarted/openshift.html) with the **-k** command line option enabled. Hopefully one day we'll be able to turn cadvisor and jadvisor into [apps](apps.html) you can easily run in the same way.

Once the above is running, the [Fabric8 Console](console.html) should have a **Metrics** tab letting you view and search the metrics via [Grafana](http://grafana.org/).


### How to enable metrics scraping in your applications

When using [Prometheus](http://prometheus.io) to [collect metrics](metrics.html) for monitoring your containers you can enable
scraping in Java Maven projects in the `pom.xml` file accordingly. For example to enable scraping and service enable this on port 9779, define the following in the `pom.xml` file:

    <fabric8.metrics.scrape>true</fabric8.metrics.scrape>
    <fabric8.metrics.port>9779</fabric8.metrics.port>
    <fabric8.metrics.scheme>http</fabric8.metrics.scheme>

There must be at least one service define in your application to ensure the metrics scaping is enabled. If no services has been defined,
then a headless service must be configured to ensure the metrics scraping works. Add the following two lines to the `pom.xml` file:

    <fabric8.service.name>${project.artifactId}</fabric8.service.name>
    <fabric8.service.headless>true</fabric8.service.headless>

This headless service is used to collect application level metrics for ingestion into Prometheus.
