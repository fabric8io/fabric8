## Developer workflow

If you tend to use an IDE for working with Java and things like [Apache Camel](http://camel.apache.org) and [Apache CXF](http://cxf.apache.org/) here's how to get started using your IDE or [Maven](http://maven.apache.org/) with [fabric8](http://fabric8.io/).

There's also a [more in depth screencast](http://www.christianposta.com/blog/?p=373) if you prefer to watch that first.

### To start create an archetype

We've got lots of [quickstarts](https://github.com/fabric8io/fabric8/tree/master/quickstarts) and [examples](https://github.com/fabric8io/fabric8/tree/master/tooling/examples) which we've converted into [Maven Archetypes](https://maven.apache.org/guides/introduction/introduction-to-archetypes.html).

So if you have [installed Maven](http://maven.apache.org/download.cgi#Installation), type the following:

    mvn org.apache.maven.plugins:maven-archetype-plugin:2.2:generate -Dfilter=io.fabric8:

This will list all the various archetypes. Pick one that suits your fancy, e.g. **io.fabric8.archetypes:cbr-archetype** for the [Camel Content Based Router Quickstart](https://github.com/fabric8io/fabric8/tree/master/quickstarts/cbr).

Then enter these values:

    groupId:    cool
    artifactId: mydemo
    version:    1.0.0.SNAPSHOT
    package:    cool

And confirm with 'Y'.

### Building on OpenShift

TODO