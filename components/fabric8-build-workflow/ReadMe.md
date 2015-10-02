## Fabric8 Build Workflow

This library provides a Build Workflow Engine using [jBPM](http://jbpm.org/) which can be used to orchestrate [OpenShift](http://openshift.github.io/) builds between environments to create a [Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) mechanism.


![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-build-workflow</artifactId>
                <version>2.2.40</version>
            </dependency>

### Overview

This library consists of 3 main parts:

#### BuildWorkItemHandler

The [BuildWorkItemHandler](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/trigger/BuildWorkItemHandler.java#L30) implements the jBPM WorkItemHandler interface to trigger a build from inside the BPM flow.

Once the build is triggered the [BuildCorrelationKey](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/BuildCorrelationKey.java#L23) of the newly created build is registered into the [BuildProcessCorrelator](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/correlate/BuildProcessCorrelator.java#L29) for later use by the [BuildSignaller](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/signal/BuildSignaller.java#L37).

#### BuildSignaller and BuildSignallerService

The [BuildSignaller](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/signal/BuildSignaller.java#L37) receives [BuildFinishedEvent](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/signal/BuildSignaller.java#L57)) events from the underlying BuildWatcher and then signals the BPM flows; either creating new process instances or signalling existing process instances

To start the [BuildSignallerService](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/signal/BuildSignallerService.java#L31) just run the following pseudo code:

    BuildSignallerService signallerService = new BuildSignallerService();
    signallerService.start();

    // lets block the main thread until its terminated
    signallerService.join();

#### BuildProcessCorrelator

The [BuildProcessCorrelator](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/correlate/BuildProcessCorrelator.java#L29) is used to store the mappings of a [BuildCorrelationKey](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/BuildCorrelationKey.java#L23) to a BPM process instance ID; so that when the [BuildSignaller](https://github.com/fabric8io/fabric8/blob/master/components/fabric8-build-workflow/src/main/java/io/fabric8/io/fabric8/workflow/build/signal/BuildSignaller.java#L37) has been notified of a build completing it can find the correlated BPM Process instance ID to signal; otherwise it signals with no process ID which usually results in a new process starting.


#### Simulator

The simulator allows you to test out the jBPM side of things without having a full OpenShift environment and set of builds to play with.

To try out the simulator setup your environment variables:

    export FABRIC8_SIMULATOR_ENABLED=true
    export FABRIC8_SIMULATOR_START_BUILD_NAME=MyBuild

Where **MyBuild** is the name of the first build to trigger in a workflow.

You can run it via:

    mvn test-compile exec:java
