## Fabric8 Developer Console

Fabric8 Developer Console provides a rich HTML5 web application for creating, building, testing and managing Microservices on [Kubernetes](http://kubernetes.io/).

Fabric8 Developer Console is single lightweight pod, [replication controller](replicationControllers.html) and [services](services.html) that runs on any [Kubernetes](http://kubernetes.io/) or [Openshift](https://www.openshift.org/) cluster.
 
### Installing

Here is [how to install the Console on Kubernetes / OpenShift](getStarted/apps.html#console)

### Authentication

The fabric8 console uses the authentication of the underlying system. Therefor you need to adjust the authentication on your base platform.

So if you are on openshift have a look how to [configure authentication](https://docs.openshift.org/latest/install_config/configuring_authentication.html).

### Getting Started

The console has a number of tabs that let you work with the various Kubernetes resources. We'll highlight the main ones you'll need to use:

#### Controllers

The main tab to get a feel for what's running in your system is the **Controllers** tab which shows all the [replication controllers](replicationControllers.html).

To scale up or down a controller to run more or less [pods](pods.html) (containers) just increase or decrease the **Desired Replicas** value and hit **Save** and hey presto pods are created or destroyed.

![controllers tab screenshot](images/controllers.png)

#### Overview

The **Overview** tab gives you a feel for how all the various [services](services.html) and  [replication controllers](replicationControllers.html) interact:

![overview tab screenshot](images/overview.png)

#### Library

The **Library** tab lets you drag and drop [Apps](apps.html) into your library from downloaded [App Zips](appzip.html) so that you can see all the available applications you can run. Click on an app then hit **Run** to run them.

You can also drag folder from the Library to your desktop and local file system to save them.

![library tab screenshot](images/library.png)


### Using Kubernetes/OpenShift

If you are using Kubernetes or OpenShift you need to find the URL for the console service. From there you should be able to navigate to the tabs for [pods](pods.html), [replication controllers] and [services](services.html).

If you use the [Fabric8 Vagrant](getStarted/vagrant.html) approach then the URL is `http://fabric8.vagrant.f8/`.

Otherwise you need to find the host name to use.

e.g. on OpenShift V3 you can find it via:

    oc get route fabric8

