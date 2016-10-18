## Logging

Logging is a key part of being able to understand and diagnose distributed systems. Consolidating log statements and events and being able to query and visualise them is an extremely useful tool.

With fabric8 we recommend using:

* [Elasticsearch](http://www.elasticsearch.com/products/elasticsearch/) as the back end storage of logs and events in your system as its easy to scale, provides replicated data for resilience and is very easy to query using complex structural and textual queries
* [Kibana](http://www.elasticsearch.com/products/kibana/) as the console to view, query and analyze the logs
* [fluentd](http://www.fluentd.org) for automated log spooling from Docker containers directly to Elasticsearch 

Using [Elasticsearch](http://www.elasticsearch.com/products/elasticsearch/) on Kubernetes makes it easy to scale the ES cluster on the server side as well as discover the cluster for clients using [Kubernetes Services](http://kubernetes.io/docs/user-guide/services/).

### How to use Logging in fabric8

If you are running Fabric8 with the [Fabric8 Console](console.html) then go to the **Apps** tab and click **Run**:

![run button in apps tab](images/logging/apps-run.png)


Then click on the "Logging" App. You can then enter the base domain name for your Kubernetes/OpenShift cluster for the `Route host name suffix` field (this will generate an OpenShift route for you. We are working generating the Kubernetes Ingress definition for vanilla Kube [#5567](https://github.com/fabric8io/fabric8/issues/5567)). You can optionally turn this off by clicking the checkbox. This should install Elasticsearch Master nodes and the Kibana UI as Kubernetes pods and fluentd as a Kubernetes Daemon set. It will also set up Kubernetes replication controllers and Services for you. For example, if your base Kubernetes cluster is at `vagrant.f8` then the App will be set up with services at `kibana.vagrant.f8` and `elasticsearch.vagrant.f8`.

![run button in apps tab](images/logging/run-logging-app.png)


Once the Logging App is running (watch the pods start up in the fabric8 console!) you can find the service you'd like to access and navigate to it. You can do this by selecting the `Services` tab, or the little vertical ellipses that has a Services drop-down chooser:

![run button in apps tab](images/logging/click-services.png)
