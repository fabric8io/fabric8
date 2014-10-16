## Apps (or Kubernetes Application Templates)

Apps (or Kubernetes Application Templates) are a Kubernetes extension proposed by OpenShift 3 to enable a single JSON file to configure various Kubernetes resources ([Pods](pods.html), [Replication Controllers](replicationControllers.html), [Services](services.html)). 

So an App could define some database containers, cache tier and application servers; it represents a typical enterprise or web application (as opposed to a mobile or desktop app).

Here's an [example template](https://github.com/openshift/origin/blob/master/api/examples/template.json). 

A template is also **parameterizable**; so that as the template is instantiated by a tool (command line, web console) the user is prompted to enter the parameter values to generate the configuration used to generate the pods, replication controllers and services. 

For example an App could generate an ActiveMQ regional cluster; where the template is parameterised with the region name, it's service port number and the number of replicas.

An App can then be packaged into an [App Zip](appzip.html) for easier distribution between environments.

