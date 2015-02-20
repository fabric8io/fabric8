## App Library

The App Library implements a RESTful [service](services.html) using a [replication controller](replicationControllers.html) to provide a wiki of [apps](apps.html) that can be run.

You can think of this as being a little like your library for Apps for your mobile device; its a way of populating your default set of Apps that are available so that you can easily run them.

The library is stored in a git based wiki and can be edited via git or via the [console](console.html).

### Features

When the App Library is running the [console](console.html):

* has a **Run...** button on the Apps tab which lets you easily pick which Apps you want to run in a few clicks
* associates any resource ([service](services.html), [replication controller](replicationControllers.html), [pod](pod.html)) with the icon stored in the App Library wiki so that things look much nicer ;)
* the Library tab allows you to explore all your installed Apps, browse the documentation and drag and drop apps from the internet or file system to or from the App Library to install or share Apps between environments or people.

### Running on Kubernetes/OpenShift

If you start [fabric8 using these instructions](openShiftDocker.html) then it should be started by default.

If not you can run the App Library on any kubernetes environment as follows:

    kube apply -f  http://central.maven.org/maven2/io/fabric8/jube/images/fabric8/app-library/2.0.29/app-library-2.0.29-kubernetes.json

Then make sure you have [run the console](console.html)
