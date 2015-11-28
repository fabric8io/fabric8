## Helm

[Helm](http://helm.sh/) is a really cool way to install and upgrade applications on Kubernetes. Its kinda like [Homebrew package manager for OS X](http://brew.sh/) but for [Kubernetes](http://kubernetes.io/).

We've been [patching](https://github.com/helm/helm/pulls/jstrachan) helm so that it can work with OpenShift Templates and adding Secret generation. 

The patches are all in the [openshift-all branch of helm](https://github.com/fabric8io/helm/tree/openshift-all). You can [download the binaries](https://bintray.com/fabric8io/helm-ci/helm/view#files/) if you want to play.

### Using Vagrant

We include helm in our [vagrant image](getStarted/vagrant.html) [here](https://github.com/fabric8io/fabric8-installer/tree/master/vagrant/openshift). So that if you recreate your vagrant image you can install/upgrade any app in fabric8. e.g. type this:

    helm upgrade fabric8/cd-pipeline

and it'll install/upgrade the [CD Pipeline chart](cdelivery.html).

### Using any OpenShift or Kubernetes environment

Just [download the binary for your platform](https://bintray.com/fabric8io/helm-ci/helm/view#files/), add it to your `$PATH` and type:

    helm update
    helm repo add fabric8 https://github.com/fabric8io/charts.git

Now helm will be pointed at the [fabric8 chart repository](https://github.com/fabric8io/charts) so you can `search`, `install`or `upgrade` the fabric8 charts.


### Using Helm

Helm refers to applications as `charts` which you can then `search`, `install`, `upgrade` or `delete` in a particular namespace in kubernetes.

Once you have got helm installed using one of the above mechansims here's how you use it:

#### Installing a chart

All the  [fabric8 charts](https://github.com/fabric8io/charts) begin with the name `fabric8/`. 

So to install a particular chart name, such as `fabric8/cd-pipeline` type:

    help install fabric8/cd-pipeline

which will install the [CD Pipeline chart](cdelivery.html).

Note that `helm install` tends to fail if it finds any resources that already exist; so use `helm upgrade` instead; which creates or upgrades depending on whats already installed!

#### Upgrading a chart

After you have installed a chart a new release can come along from time to time. So when a new fabric8 release comes out if you want to upgrade your installation type:

    helm update
    helm upgrade fabric8/cd-pipeline

and it upgrades, recreating/updating all the kubernetes resources! 

Helm looks and feels kinda like homebrew; so `helm update` pulls down all the latest git repos of the charts (helm speak for 'apps`). e.g. all our fabric8 charts are here: [https://github.com/fabric8io/charts
](https://github.com/fabric8io/charts)


