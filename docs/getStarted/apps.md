## Install Fabric8 on OpenShift

[Fabric8 apps](../fabric8Apps.html) have been
[packaged](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.96/distro-2.2.96-templates.zip) to make them easy
to install on Kubernetes or OpenShift.

### Install via the console

If you are already running the [fabric8 console](../console.html) you can use it to install all the
other [Fabric8 apps](../fabric8Apps.html) and quickstarts. If you are not yet running the
fabric8 console then try [install it via the CLI below](#console).

When you open the fabric8 console select the `Apps` tab then click on the `Run...` button (top right green butotn).

This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
on your installation.

* To Run any of the installed templates just click the `Run` button (the green play button).
* To install any new [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) or
  other Kubernetes resources just drag and drop the JSON file onto the `Apps` tab!
* You can download the [fabric8 templates 2.2.96 distribution](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.96/distro-2.2.96-templates.zip)
  unzip and drag the JSON files you want to install onto the [fabric8 console](../console.html)
  and they should appear on the `Run...` page  
* You can also install other OpenShift Templates or Kubernetes resources via the **oc** command line tool:

        oc create -f jsonOrYamlFileOrUrl

### Install via the CLI

These instructions assume that you have either

* Either setup the fabric8 [Vagrant image](vagrant.html) or have a [native OpenShift](openshift.html) installation
* You have the [local machine](local.html)  setup to talk to OpenShift

#### Setup domain

* setup the **KUBERNETES_DOMAIN** environment variable for the domain you are installing to. Usually this is a host name or domain name.

e.g. if you are using the fabric8 Vagrant image then use

```
export KUBERNETES_DOMAIN=vagrant.f8
```

#### Downloading all templates

Download and unzip the [fabric8 templates 2.2.96 distribution](http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.96/distro-2.2.96-templates.zip).

e.g.

```
curl -o fabric8.zip http://repo1.maven.org/maven2/io/fabric8/devops/distro/distro/2.2.96/distro-2.2.96-templates.zip
unzip fabric8.zip
cd main
```

Once you have found the `kubernetes.json` file for the [app](fabric8Apps.html) you wish to install type the following using the `oc` command from OpenShift:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f kubernetes.json | oc create -f -

For example to install the [fabric8 console](console.html) then type:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f console-2.2.96.json | oc create -f -

#### Download templates individually

Or to install from the central repository then choose the commands below to suit the application you wish to install:

##### Console

Provides the base [fabric8 console](../console.html) at the `vagrant.f8` domain.

[gofabric8](https://github.com/fabric8io/gofabric8) provides a script for installing fabric8 on openshift or vanilla kubernetes.

First download a [gofabric8 binary](https://github.com/fabric8io/gofabric8/releases), unpack the archive and copy the binary to a folder in your path, e.g. /usr/bin/

Then install fabric8 by running:

```
gofabric8 deploy -y
gofabric8 secrets -y
```

##### Management

Provides centralised [Logging](logging.html) and [Metrics](metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/management/2.2.96/management-2.2.96-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

###### Logging

Provides just the centralised [Logging](../logging.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/logging/2.2.96/logging-2.2.96-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

###### Metrics

Provides just the centralised [Metrics](../metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/metrics/2.2.96/metrics-2.2.96-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### iPaaS

Provides the [fabric8 console](../console.html) and the [Integration Platform As A Service](../ipaas.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://central.maven.org/maven2/io/fabric8/apps/ipaas/2.2.96/ipaas-2.2.96-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Continuous Delivery

Provides a Continuous Integration and [Continuous Delivery](../cdelivery.html) system.

The complete [Continuous Delivery](../cdelivery.html) installation.

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/cd-pipeline/2.2.96/cd-pipeline-2.2.96-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

### Creating Routes

If you install via the command line then you will need to create the
[OpenShift Routes](http://docs.openshift.org/latest/admin_guide/router.html) for any [services](../services.html)
you created. Note that this is done automatically if you create applications via the [fabric8 console](../console.html).

To do this use the [mvn fabric8:create-routes](../mavenFabric8CreateRoutes.html) goal.

Note that this command can be run in any folder; it doesn't need to be run from inside a maven project.

Before you start make sure you have [setup your local machine](local.html) have logged in
and setup the environment variables etc.

If you have defined the [$KUBERNETES_DOMAIN](#setup-domain) environment variable then you can use the following command:

    mvn io.fabric8:fabric8-maven-plugin:2.2.96:create-routes

Otherwise you can be specific and specify the domain you wish to use:

    mvn io.fabric8:fabric8-maven-plugin:2.2.96:create-routes -Dfabric8.domain=my.acme.com

You could then setup a wildcard DNS rule on `*.$KUBERNETES_DOMAIN` to point to the IP address of your OpenShift
master or HAProxy installation. Or you could add custom entries to your `/etc/hosts` file for each service.

If you are using the Vagrant image with Linux or OSX you already have wildcard DNS resoultion. For Windows you have to
add each new route to `%WINDIR%\System32\drivers\etc\hosts`. E.g. if your IP address for the OpenShift
master/router is `172.28.128.4` (which it is for the fabric8 [Vagrant image](vagrant.html)) then
add this to your hosts file to expose  the routes as host names:

		172.28.128.4 vagrant.f8 fabric8.vagrant.f8 docker-registry.vagrant.f8 gogs-http.vagrant.f8
		172.28.128.4 gogs-ssh.vagrant.f8 nexus.vagrant.f8 jenkins.vagrant.f8 kibana.vagrant.f8

You should now be able to access the console at [http://fabric8.vagrant.f8/](http://fabric8.vagrant.f8/)
