This profile enables Java 8 as the runtime when using [Java Containers](http://fabric8.io/gitbook/javaContainer.html), [Process Containers](http://fabric8.io/gitbook/processContainer.html) or [Docker Containers](http://fabric8.io/gitbook/docker.html).

When not using docker, you must specify the FABRIC8_JAVA8_HOME environment variable to point to the location of the JAVA_HOME for the Java 8 distribution. e.g. on OS X this would be:

    export FABRIC8_JAVA8_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0.jdk/Contents/Home