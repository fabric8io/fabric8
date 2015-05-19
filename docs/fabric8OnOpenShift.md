## Install Fabric8 on OpenShift

[Fabric8 apps](fabric8Apps.html) have been packaged to make them easy to install on OpenShift.

### Requirements

* Fabric8 currently requires [OpenShift V3 0.5.1 or later](http://www.openshift.org/).
* For [osc and osadm](https://github.com/openshift/origin/blob/master/docs/cli.md) to be in your `PATH`

The following commands assume you are on the OpenShift master machine in a folder containing the `openshift.local.config` directory:

* Enable the `cluster-admin` role to user `admin`

```
osadm policy add-cluster-role-to-user cluster-admin admin --config=openshift.local.config/master/admin.kubeconfig
```

* each namespace you wish to install fabric8 into typically requires the `openshift-cert-secrets`


You'll also need to login and switch to the correct project (namespace):

```
osc login
osc project cheese
```

Then run this command:

```
cat <<EOF | osc create -f -
---
	apiVersion: "v1beta3"
	kind: "Secret"
	metadata:
		name: "openshift-cert-secrets"                                                                                                                                                          
	data:
		root-cert: "$(base64 -w 0 openshift.local.config/master/ca.crt)"
		admin-cert: "$(base64 -w 0 openshift.local.config/master/admin.crt)"
		admin-key: "$(base64 -w 0 openshift.local.config/master/admin.key)"
EOF
```


### Install

One you have found the `kubernetes.json` file for the [app](fabric8Apps.html) you wish to install type the following using the `osc` command from OpenShift:
 
		osc process -f kubernetes.json | osc create -f -

Or to install the current releases then choose the commands below to suit the application you wish to install:

#### Base

Provides the base [fabric8 console](console.html)

		osc process http://central.maven.org/maven2/io/fabric8/apps/base/2.1.1-SNAPSHOT/base-2.1.1-kubernetes.json | osc create -f -

#### Management

Provides centralised [Logging](logging.html) and [Metrics](metrics.html)

		osc process http://central.maven.org/maven2/io/fabric8/apps/management/2.1.1-SNAPSHOT/management-2.1.1-kubernetes.json | osc create -f -

##### Logging

Provides just the centralised [Logging](logging.html)

		osc process http://central.maven.org/maven2/io/fabric8/apps/logging/2.1.1-SNAPSHOT/logging-2.1.1-kubernetes.json | osc create -f -

##### Metrics

Provides just the centralised [Metrics](metrics.html)

		osc process http://central.maven.org/maven2/io/fabric8/apps/metrics/2.1.1-SNAPSHOT/metrics-2.1.1-kubernetes.json | osc create -f -

#### iPaaS

Provides the [fabric8 console](console.html) and the [Integration Platform As A Service](ipaas.html)_

		osc process http://central.maven.org/maven2/io/fabric8/apps/ipaas/2.1.1-SNAPSHOT/ipaas-2.1.1-kubernetes.json | osc create -f -

#### Continuous Delivery

Provides a Continuous Integration and [Continuous Delivery](cdelivery.html) system.

##### CD Core

The core[Continuous Delivery](cdelivery.html) installation for building including Gogs for git hosting, Jenkins for building and Nexus as a repository manager.

		osc process http://central.maven.org/maven2/io/fabric8/apps/cdelivery-core/2.1.1-SNAPSHOT/cdelivery-core-2.1.1-kubernetes.json | osc create -f -
 
##### CD Full

The complete [Continuous Delivery](cdelivery.html) installation including **CD Core** plus the social apps like [chat such as Hubot](chat.html).

		osc process http://central.maven.org/maven2/io/fabric8/apps/cdelivery/2.1.1-SNAPSHOT/cdelivery-2.1.1-kubernetes.json | osc create -f -
 
#### Kitchen Sink

Provides all of the above!

		osc process http://central.maven.org/maven2/io/fabric8/apps/kitchen-sink/2.1.1-SNAPSHOT/kitchen-sink-2.1.1-kubernetes.json | osc create -f -


### Creating Routes

Its likely after installing any of the above applications that there will be Kubernetes [services](services.html) running that you wish to expose via [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html).

To do this use the [mvn fabric8:create-routes](mavenFabric8CreateRoutes.html) goal. e.g.

    mvn io.fabric8:fabric8-maven-plugin:2.1.1:create-routes -Dfabric8.domain=my.acme.com

You could then setup a wildcard DNS rule on `*.my.acme.com` to point to the IP address of your OpenShift master or haproxy installation. Or you could add custom entries to your `/etc/hosts` file for each service.
                                                                                                         
e.g. if your IP address for the OpenShift master/router is `127.0.0.1` then add this to your `/etc/hosts` to expose the `foo` and `bar` services:

		127.0.0.1 foo.my.acme.com ba.my.acme.com
 

