## Monitoring

Capturing historical metrics is essential to diagnose issues involving your microservices. Its also very useful for [auto scaling](elasticity.html)

So we recommend running the [Metrics microservice](../metrics.html) which uses * [Prometheus](http://prometheus.io/) as the back end storage service and REST API and then [Grafana](http://grafana.org/) as the console to view, query and analyze metrics.

If you use Java then using a [JMX Exporter YAML file](https://github.com/prometheus/jmx_exporter) will configure which JMX metrics to export to Prometheus.