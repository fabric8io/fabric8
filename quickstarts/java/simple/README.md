# Simple Java container QuickStart

This example assumes that you have a running openshift v3, and fabric8 v2 using docker. For more info see http://fabric8.io/v2/getStarted.html.

This example shows how to start the Java Container using your custom main class.

This example is implemented using simple Java SE code. The source code is provided in the following java file `src/main/java/io/fabric8/quickstarts/java/simple/Main.java`, which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/java/simple/src/main/java/io/fabric8/quickstarts/java/simple/Main.java).

This example is printing *Hello Fabric8! Here's your random string: lRaNR* to the standard output in the infinite loop.


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build and run from the source code:

1. Change your working directory to `quickstarts/java/simple` directory.
1. Run `./build-and-run.sh` to build and run the quickstart.


### Using the web console

*Note - at the moment I do not see any content under the wiki console yet.*

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `java` --> `simple`
1. Click the `New` button in the top right corner
1. In the Create New Container page, enter `mychild` in the Container Name field, and click the *Create and start container* button

## How to try this example

1. Using the hawtio console at http://dockerhost:8484/hawtio, you should now see simple java as a 'pod' under the kubernetes tab http://dockerhost:8484/hawtio/kubernetes/pods?_id=
1. You can find out the containerId using `docker ps` - the output should look something like:
```
	CONTAINER ID        IMAGE                                                   COMMAND                CREATED             STATUS              PORTS                    NAMES
	19d5280bf22e        dockerhost:5000/quickstart/java-simple:2.0.0-SNAPSHOT   /bin/sh -c 'java $JA   3 days ago          Up 28 minutes                                k8s_quickstart-java-simple.6c2fc62f_c6fa4631-588f-11e4-8746-406c8f215ad7.etcd_c6fa4631-588f-11e4-8746-406c8f215ad7_e28ca78b
```
1. Use `docker logs -f <CONTAINER ID>` to see what the simple java app is writing to the console, and
see that every second it logs another line:
```
	Hello Fabric8! Here's your random string: NMsq3
	Hello Fabric8! Here's your random string: wPzMW
	Hello Fabric8! Here's your random string: tiTpX
```
1. At this point you have verified that things work but additionally we can find out some more about this container.
First `docker inspect <image>` to see all the metadata. For instance part of this output reads
```
....
     "Env": [
            "HOME=/",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "JOLOKIA_VERSION=1.2.2",
            "CLASSPATH=/maven/*:/maven",
            "JAVA_OPTIONS=-javaagent:/opt/jolokia/jolokia.jar=host=0.0.0.0,port=8778,agentId=$HOSTNAME",
            "MAIN=io.fabric8.quickstarts.java.simple.Main"
        ],
        "Cmd": [
            "/bin/sh",
            "-c",
            "java $JAVA_OPTIONS -cp $CLASSPATH $MAIN $ARGUMENTS"
        ],
....
```
which shows our java process is started using a Cmd, and that it is uing the io.fabric8.quickstarts.java.simple.Main class to start it, with a classpath of everything in the /maven directory. The JAVA_OPTION start a jolokia agent.

1. Using `docker top d356604acb80` we can see simple java process as well as the jolokia agent.
```
	PID                 USER                COMMAND
	2624                root                /bin/sh -c java $JAVA_OPTIONS -cp $CLASSPATH $MAIN $ARGUMENTS
	2643                root                java -javaagent:/opt/jolokia/jolokia.jar=host=0.0.0.0,port=8778,agentId=$HOSTNAME -cp /maven/*:/maven io.fabric8.quickstarts.java.simple.Main
```
1. You use NSEnter (https://github.com/jpetazzo/nsenter) to connect to the container using the alias `docker-enter <containerId>`. We can inspect the file system and see our jars in the /maven directory 
```
	/maven# ls -l
	total 412
	-rw-r--r-- 1 root root 412739 Oct 20  2014 commons-lang3-3.3.2.jar
	-rw-r--r-- 1 root root   4453 Oct 20  2014 java-simple-2.0.0-SNAPSHOT.jar
```
and the jolokia jars in the /opt/jolokia directory
```
	opt/jolokia# ls -l
	total 388
	-rw-r--r-- 1 root root 389459 Jun 14 17:43 jolokia.jar
	-rw-r--r-- 1 root root    871 Oct  8 09:33 jolokia_env.sh
```

## Undeploy this example

The following information is divided into two sections, whether you are using the command line shell in fabric, or using the web console

### Using the OpenShift Kube command line shell

To stop and undeploy the example using the OpenShift Kube command line, enter the following commands at the console:

        openshift kube stop quickStartJavaSimpleController
        openshift kube rm quickStartJavaSimpleController

### Using the web console

To stop and undeploy the example in fabric8:

1. In the web console, click the *Runtime* button in the navigation bar.
1. Select the `mychild` container in the *Containers* list, and click the *Stop* button in the top right corner
