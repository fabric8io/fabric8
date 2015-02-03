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

You will also need to run the cadvisor/jadvisor containers on each node to ensure the metrics are collected. This is done automatically if you [use the bash script to install fabric8](openShiftDocker.html) with the **-k** command line option enabled. Hopefully one day we'll be able to turn cadvisor and jadvisor into [apps](apps.html) you can easily run in the same way.

Once the above is running, the [Fabric8 Console](console.html) should have a **Metrics** tab letting you view and search the metrics via [Grafana](http://grafana.org/).