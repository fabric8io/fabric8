## Apps (or Application Templates)

Apps (or Application Templates) are a Kubernetes extension proposed by OpenShift 3 to enable a single JSON file to configure various resources within the standard Kubernetes ([Pods](pods.html), [Replication Controllers](replicationControllers.html), [Services](services.md)).

Here's an [example template](https://github.com/openshift/origin/blob/master/api/examples/template.json). A template is also parameterizable with parameters; so that as the template is instantiated a tool (command line, web based) can prompt users to enter the parameter values used to realise the configuration so it can be created on Kubernetes.



