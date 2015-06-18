## Install Fabric8 on OpenShift

[Fabric8 apps](fabric8Apps.html) have been packaged to make them easy to install on OpenShift.

### Requirements

These instructions assume that you have 
* [Setup OpenShift](setupOpenShift.html) with version [OpenShift V3 0.5.1 or later](http://www.openshift.org/). We recommend using [Fabric8 Vagrant Image](openShiftWithFabric8Vagrant.html)
* [Setup your local machine to talk to OpenShift](setupLocalHost.html) 

#### Setup domain

* setup the **KUBERNETES_DOMAIN** environment variable for the domain you are installing to. Usually this is a host name or domain name.

e.g. if you are using the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html) then use

```
export KUBERNETES_DOMAIN=vagrant.local
```

If not use something unique such as

```
export KUBERNETES_DOMAIN=fabric8.local
```

### Install Fabric8 Apps

Once you have found the `kubernetes.json` file for the [app](fabric8Apps.html) you wish to install type the following using the `osc` command from OpenShift:
 
		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f kubernetes.json | osc create -f -

Or to install the current releases then choose the commands below to suit the application you wish to install:

#### Base

Provides the base [fabric8 console](console.html)

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/base/2.2.0/base-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### Management

Provides centralised [Logging](logging.html) and [Metrics](metrics.html)

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/management/2.2.0/management-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Logging

Provides just the centralised [Logging](logging.html)

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/logging/2.2.0/logging-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Metrics

Provides just the centralised [Metrics](metrics.html)

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/metrics/2.2.0/metrics-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### iPaaS

Provides the [fabric8 console](console.html) and the [Integration Platform As A Service](ipaas.html)

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/ipaas/2.2.0/ipaas-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### Continuous Delivery

Provides a Continuous Integration and [Continuous Delivery](cdelivery.html) system.

##### CD Core

The core[Continuous Delivery](cdelivery.html) installation for building including Gogs for git hosting, Jenkins for building and Nexus as a repository manager.

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/cdelivery-core/2.2.0/cdelivery-core-2.2.0-kubernetes.json \
		| osc create -f -
 
Then [setup the OpenShift Routes](#creating-routes)

##### CD Full

The complete [Continuous Delivery](cdelivery.html) installation including **CD Core** plus the social apps like [chat such as Hubot](chat.html).

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/cdelivery/2.2.0/cdelivery-2.2.0-kubernetes.json \
		| osc create -f -
 
Then [setup the OpenShift Routes](#creating-routes)

#### Kitchen Sink

Provides all of the above!

		osc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/kitchen-sink/2.2.0/kitchen-sink-2.2.0-kubernetes.json \
		| osc create -f -

Then [setup the OpenShift Routes](#creating-routes)

### Creating Routes

Its likely after installing any of the above applications that there will be Kubernetes [services](services.html) running that you wish to expose via [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html).

To do this use the [mvn fabric8:create-routes](mavenFabric8CreateRoutes.html) goal. 

Note that this command can be run in any folder; it doesn't need to be run from inside a maven project.

Before you start make sure you have [setup your local machine](setupLocalHost.html) have logged in and setup the environment variables etc.

If you have defined the [$KUBERNETES_DOMAIN environment variable](#setup-domain) then you can use the following command:

    mvn io.fabric8:fabric8-maven-plugin:2.1.3:create-routes

Otherwise you can be specific and specify the domain you wish to use:

    mvn io.fabric8:fabric8-maven-plugin:2.1.3:create-routes -Dfabric8.domain=my.acme.com

You could then setup a wildcard DNS rule on `*.$KUBERNETES_DOMAIN` to point to the IP address of your OpenShift master or haproxy installation. Or you could add custom entries to your `/etc/hosts` file for each service.
                                                                                                         
e.g. if your IP address for the OpenShift master/router is `172.28.128.4` (which it is for the [fabric8 vagrant image](openShiftWithFabric8Vagrant.html)) then add this to your `/etc/hosts` to expose the routes as host names:

		172.28.128.4 vagrant.local fabric8.vagrant.local fabric8-master.vagrant.local docker-registry.vagrant.local gogs-http.vagrant.local gogs-ssh.vagrant.local nexus.vagrant.local jenkins.vagrant.local kibana.vagrant.local

You should now be able to access the console at [http://fabric8.vagrant.local/](http://fabric8.vagrant.local/)
