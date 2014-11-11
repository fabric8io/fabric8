## Install OpenShift Natively

These instructions walk you through natively installing OpenShift on your host operating system.

**NOTE** OpenShift used to work in Linux, Windows and OS X; but recent networking/service changes only work on Linux. So until that is fixed if you don't use Linux we recommend you try using [OpenShift with Docker](openShiftDocker.md) or [OpenShift with Vagrant](openShiftVagrant.md)

### Downloading Openshift V3

You can download a [distribution of OpenShift V3](https://github.com/openshift/origin/releases) if there is a download
for your platform (currently OpenShift runs only on the 64-bit Linux). We recommend to download the <a href="https://github.com/openshift/origin/releases/download/20141003/openshift-origin-linux64-e4d4ecf.tar.gz">alpha nightly 20141003/e4d4ecf</a> version of the
OpenShift 3 - Fabric8 has been tested against it.

### Building OpenShift V3

Its actually pretty quick and easy to build OpenShift yourself:

* install [go lang](http://golang.org/doc/install)
* [compile the OpenShift V3 code](https://github.com/jstrachan/origin/blob/master/README.md#getting-started) via this command:

```
    hack/build-go.sh
```

* Add the **_output/go/bin** folder to your **PATH** so that you can run the **openshift** command from any directory. e.g. add this to your ~/.bashrc

```
    export PATH=$PATH:$GOPATH/src/github.com/openshift/origin/_output/go/bin
```

### Next Steps

* [Setup Your Machine for a local OpenShift Installation](setupMachine.html)
* [Run Fabric8 on your local OpenShift Installation](runFabric.html)



