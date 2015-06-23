## Install Fabric8 on OpenShift

[Fabric8 apps](fabric8Apps.html) have been [packaged](http://repo1.maven.org/maven2/io/fabric8/apps/distro/2.2.2/distro-2.2.2-templates.zip) to make them easy to install on Kubernetes or OpenShift.

### Install via the console

If you are already running the [fabric8 console](http://fabric8.io/guide/console.html) you can use it to install all the other [Fabric8 apps](fabric8Apps.html) and quickstarts. If you are not yet running the [fabric8 console](http://fabric8.io/guide/console.html) then try [install it via the CLI below](#console).

When you open the [fabric8 console](http://fabric8.io/guide/console.html) select the `Apps` tab then click on the `Run...` button (top right green butotn). 

This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) on your installation.

* To Run any of the installed templates just click the `Run` button (the green play button).
* To install any new [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) or other Kubernetes resources just drag and drop the JSON file onto the `Apps` tab! 
  * You can download the [fabric8 templates 2.2.2 distribution](http://repo1.maven.org/maven2/io/fabric8/apps/distro/2.2.2/distro-2.2.2-templates.zip) unzip and drag the JSON files you want to install onto the [fabric8 console](http://fabric8.io/guide/console.html) and they should appear on the `Run...` page  
* You can also install other OpenShift Templates or Kubernetes resources via the **oc** command line tool:

    oc create -f jsonOrYamlFileOrUrl

### Install via the CLI

These instructions assume that you have either
 
* [Fabric8 Vagrant](getStartedVagrant.md) and setup your local machine
* [Setup OpenShift](setupOpenShift.html) with version [OpenShift V3 1.0 or later](http://www.openshift.org/) and then [setup your local machine to talk to OpenShift](setupLocalHost.html) 

#### Setup domain

* setup the **KUBERNETES_DOMAIN** environment variable for the domain you are installing to. Usually this is a host name or domain name.

e.g. if you are using the [fabric8 vagrant image](getStartedVagrant.html) then use

```
export KUBERNETES_DOMAIN=vagrant.f8
```

#### Downloading the templates

Download and unzip the [fabric8 templates 2.2.2 distribution](http://repo1.maven.org/maven2/io/fabric8/apps/distro/2.2.2/distro-2.2.2-templates.zip).

e.g.

```
curl -o fabric8.zip http://repo1.maven.org/maven2/io/fabric8/apps/distro/2.2.2/distro-2.2.2-templates.zip
unzip fabric8.zip
cd main
```

Once you have found the `kubernetes.json` file for the [app](fabric8Apps.html) you wish to install type the following using the `oc` command from OpenShift:
 
		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f kubernetes.json | oc create -f -

For example to install the [fabric8 console](console.html) then type:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f base-2.2.2.json | oc create -f -

Or to install from the central repository then choose the commands below to suit the application you wish to install:


#### Console

Provides the base [fabric8 console](console.html) at the `vagrant.f8` domain:

		export KUBERNETES_DOMAIN=vagrant.f8
		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/base/2.2.2/base-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### Management

Provides centralised [Logging](logging.html) and [Metrics](metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/management/2.2.2/management-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Logging

Provides just the centralised [Logging](logging.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/logging/2.2.2/logging-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Metrics

Provides just the centralised [Metrics](metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/metrics/2.2.2/metrics-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### iPaaS

Provides the [fabric8 console](console.html) and the [Integration Platform As A Service](ipaas.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/ipaas/2.2.2/ipaas-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

#### Continuous Delivery

Provides a Continuous Integration and [Continuous Delivery](cdelivery.html) system.

##### CD Core

The core[Continuous Delivery](cdelivery.html) installation for building including Gogs for git hosting, Jenkins for building and Nexus as a repository manager.

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/cdelivery-core/2.2.2/cdelivery-core-2.2.2-kubernetes.json \
		| oc create -f -
 
Then [setup the OpenShift Routes](#creating-routes)

##### CD Full

The complete [Continuous Delivery](cdelivery.html) installation including **CD Core** plus the social apps like [chat such as Hubot](chat.html).

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/cdelivery/2.2.2/cdelivery-2.2.2-kubernetes.json \
		| oc create -f -
 
Then [setup the OpenShift Routes](#creating-routes)

#### Kitchen Sink

Provides all of the above!

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/kitchen-sink/2.2.2/kitchen-sink-2.2.2-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

### Creating Routes

If you install via the command line then you will need to create the [OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) for any [services](services.html) you created. Note that this is done automatically if you create applications via the [fabric8 console](console.html).

To do this use the [mvn fabric8:create-routes](mavenFabric8CreateRoutes.html) goal. 

Note that this command can be run in any folder; it doesn't need to be run from inside a maven project.

Before you start make sure you have [setup your local machine](setupLocalHost.html) have logged in and setup the environment variables etc.

If you have defined the [$KUBERNETES_DOMAIN environment variable](#setup-domain) then you can use the following command:

    mvn io.fabric8:fabric8-maven-plugin:2.2.2:create-routes

Otherwise you can be specific and specify the domain you wish to use:

    mvn io.fabric8:fabric8-maven-plugin:2.2.2:create-routes -Dfabric8.domain=my.acme.com

You could then setup a wildcard DNS rule on `*.$KUBERNETES_DOMAIN` to point to the IP address of your OpenShift master or haproxy installation. Or you could add custom entries to your `/etc/hosts` file for each service.
                                                                                                         
e.g. if your IP address for the OpenShift master/router is `172.28.128.4` (which it is for the [fabric8 vagrant image](getStartedVagrant.html)) then add this to your `/etc/hosts` to expose the routes as host names:

		172.28.128.4 vagrant.f8 fabric8.vagrant.f8 fabric8-master.vagrant.f8 docker-registry.vagrant.f8 gogs-http.vagrant.f8 gogs-ssh.vagrant.f8 nexus.vagrant.f8 jenkins.vagrant.f8 kibana.vagrant.f8

You should now be able to access the console at [http://fabric8.vagrant.f8/](http://fabric8.vagrant.f8/)
