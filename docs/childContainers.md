## Child Containers

Child containers refers to an [Apache Karaf](http://karaf.apache.org/) concept of _child_ containers; which basically means a separate child process on the same machine as a _root_ container; with the root container starting and stopping the child containers and the children sharing some disk space (installation jars etc) with the root container to minimise disk footprint.

Child containers are the most common containers you'll probably use since you'll probably start playing with fabric8 on your laptop. e.g. if you enable the [Auto Scaler](http://fabric8.io/gitbook/requirements.html) and define some requirements then the fabric you spin up on your laptop will automatically create some child containers (i.e. child processes).

Traditionally child containers were Karaf only; but now we support child [Process Containers](http://fabric8.io/gitbook/processContainer.html) and child [Java Containers](http://fabric8.io/gitbook/javaContainer.html).
