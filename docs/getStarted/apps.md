## Installing Fabric8 Apps

### Using gofabric8

The easiest way to install all the OpenShift templates for fabric8 into your OpenShift cluster and setup the required ServiceAccounts, security and secrets is via the [gofabric8 command line tool](https://github.com/fabric8io/gofabric8/).

Download and install [gofabric8](https://github.com/fabric8io/gofabric8/releases) and ensure its on your `$PATH`.

Test that your machine can connect to the OpenShift cluster via

```
oc get pods
```

If that fails you might want to try using `oc login` to login to your cluster again.

Also make sure you are in the right project you wish to install fabric8. Use `oc project` to list or change the project and `oc new-project` to create a new project.

Then to install the [fabric8 developer console](../console.html) type this:

```
gofabric8 deploy -y --domain=XXX
gofabric8 secrets -y
```

where `XXX` should be the domain postfix to access applications on your OpenShift cluster.

If you wish to install the full [Fabric8 Microservices Platform with CI / CD support](../cdelivery.html) then try this command instead:

```
gofabric8 deploy -y --domain=XXX --app=cd-pipeline
gofabric8 secrets -y
```

It will take a few moments to startup and download the [Fabric8 Developer Console](../console.html), you should see the pod startup in the OpenShift console or via the commmand:
```
oc get pods -w
```

Now that the fabric8 console is up and running you should be able to access it at: http://fabric8.${DOMAIN}/ based on your domain.

From there you should be able to start running apps and having fun! :)

### Troubleshooting

Check out the [troubleshooting guide](troubleshooting.html) for more help.

### Install via the console

If you are already running the [fabric8 console](../console.html) you can use it to install all the other [Fabric8 apps](../fabric8Apps.html). If you are not yet running the
fabric8 console then try [install it via the CLI above](#using-gofabric8).

When you open the fabric8 console select a `Team` page (which views a namespace in kubernetes), then select the `Runtie` tab then click on the `Run...` button (top right green button).

This will list all of the installed [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html)
on your installation.

* To Run any of the installed templates just click the `Run` button (the green play button).
* To install any new [OpenShift Templates](http://docs.openshift.org/latest/dev_guide/templates.html) or
  other Kubernetes resources just drag and drop the JSON file onto the `Apps` tab!
* You can download the [fabric8 templates 2.2.153 distribution](http://repo1.maven.org/maven2/io/fabric8/forge/distro/distro/2.2.153/distro-2.2.153-templates.zip)
  unzip and drag the JSON files you want to install onto the [fabric8 console](../console.html)
  and they should appear on the `Run...` page  
* You can also install other OpenShift Templates or Kubernetes resources via the **oc** command line tool:

        oc create -f jsonOrYamlFileOrUrl

### Install via the oc CLI

These instructions assume that you have either

* Either setup the fabric8 [Vagrant image](vagrant.html) or have a [native OpenShift](openshift.html) installation
* You have the [local machine](local.html) setup to talk to OpenShift

#### Using the imported templates

When you install fabric8 via the [gofabric8 command line tool](#using-gofabric8) it will have installed openshift templates for all the Microservices and packages in fabric8. You can view them via

```
oc get template
```

You can then run a template, such as `cd-pipeline` via:

```
oc process cd-pipeline | oc create -f -
```


#### Running templates without gofabric8

While its not recommended; you can try to run templates by hand without the [gofabric8 command line tool](#using-gofabric8)

These instructions describe how:

##### Setup domain environment variable

* setup the **KUBERNETES_DOMAIN** environment variable for the domain you are installing to. Usually this is a host name or domain name.

e.g. if you are using the fabric8 Vagrant image then use

```
export KUBERNETES_DOMAIN=vagrant.f8
```

##### Downloading all templates

Download and unzip the [fabric8 templates 2.2.153 distribution](http://repo1.maven.org/maven2/io/fabric8/forge/distro/distro/2.2.153/distro-2.2.153-templates.zip).

e.g.

```
curl -o fabric8.zip http://repo1.maven.org/maven2/io/fabric8/forge/distro/distro/2.2.153/distro-2.2.153-templates.zip
unzip fabric8.zip
cd main
```

Once you have found the `kubernetes.json` file for the [app](fabric8Apps.html) you wish to install type the following using the `oc` command from OpenShift:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f kubernetes.json | oc create -f -

For example to install the [fabric8 console](console.html) then type:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f console-2.2.101.json | oc create -f -

##### Download templates individually

Or to install from the central repository then choose the commands below to suit the application you wish to install:

Then you can run individual Microservices as follows:

##### Management

Provides centralised [Logging](logging.html) and [Metrics](metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/management/2.2.101/management-2.2.101-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

###### Logging

Provides just the centralised [Logging](../logging.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/logging/2.2.101/logging-2.2.101-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

###### Metrics

Provides just the centralised [Metrics](../metrics.html)

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/devops/packages/metrics/2.2.101/metrics-2.2.101-kubernetes.json \
		| oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### iPaaS

Provides the [fabric8 console](../console.html) and the [Integration Platform As A Service](../ipaas.html)

Download and unzip the [fabric8 templates 2.2.94 distribution](http://repo1.maven.org/maven2/io/fabric8/ipaas/distro/distro/2.2.94/distro-2.2.94-templates.zip).

e.g.

```
curl -o fabric8.zip http://repo1.maven.org/maven2/io/fabric8/ipaas/distro/distro/2.2.94/distro-2.2.94-templates.zip
unzip fabric8.zip
cd main
```
Then run whatever templates you want to run from the ipaas:

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f apiman-2.2.94.json | oc create -f -

Then [setup the OpenShift Routes](#creating-routes)

##### Continuous Delivery

Provides a Continuous Integration and [Continuous Delivery](../cdelivery.html) system.

The complete [Continuous Delivery](../cdelivery.html) installation.

		oc process -v DOMAIN=$KUBERNETES_DOMAIN -f \
		http://repo1.maven.org/maven2/io/fabric8/forge/packages/cd-pipeline/2.2.153/cd-pipeline-2.2.153-kubernetes.json \
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

    mvn io.fabric8:fabric8-maven-plugin:2.2.101:create-routes

Otherwise you can be specific and specify the domain you wish to use:

    mvn io.fabric8:fabric8-maven-plugin:2.2.101:create-routes -Dfabric8.domain=my.acme.com

You could then setup a wildcard DNS rule on `*.$KUBERNETES_DOMAIN` to point to the IP address of your OpenShift
master or HAProxy installation. Or you could add custom entries to your `/etc/hosts` file for each service.

If you are using the Vagrant image with Linux or OSX you already have wildcard DNS resoultion. For Windows you have to
add each new route to `%WINDIR%\System32\drivers\etc\hosts`. E.g. if your IP address for the OpenShift
master/router is `172.28.128.4` (which it is for the fabric8 [Vagrant image](vagrant.html)) then
add this to your hosts file to expose  the routes as host names:

		172.28.128.4 vagrant.f8 fabric8.vagrant.f8 docker-registry.vagrant.f8 gogs-http.vagrant.f8
		172.28.128.4 gogs-ssh.vagrant.f8 nexus.vagrant.f8 jenkins.vagrant.f8 kibana.vagrant.f8

You should now be able to access the console at [http://fabric8.vagrant.f8/](http://fabric8.vagrant.f8/)
