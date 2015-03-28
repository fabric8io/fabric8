## Fabric8 Build Workflow

This library provides a Build Workflow Engine using [jBPM](http://jbpm.org/) which can be used to orchestrate [OpenShift](http://openshift.github.io/) builds between environments to create a [Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) mechanism.


![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

            <dependency>
                <groupId>io.fabric8</groupId>
                <artifactId>fabric8-build-workflow</artifactId>
                <version>2.0.32</version>
            </dependency>

### Overview

This library consists of 3 main parts:

#### BuildWorkItemHandler

This WorkItemHandler triggers a build from inside the BPM flow.

#### BuildSignaller and BuildSignallerService

The BuildSignaller receives BuildFinishedEvent events from the underlying BuildWatcher and then signals the BPM flows; either creating new process instances or signalling existing process instances

To start the BuildSignallerService just run the following pseudo code:

BuildSignallerService signallerService = new BuildSignallerService();
signallerService.start();

// lets block the main thread until its terminated
signallerService.join();

#### BuildProcessCorrelator

The BuildProcessCorrelator is used to store the mappings of a BuildCorrelationKey to a BPM process instance ID; so that when the BuildSignaller has been notified of a build completing it can find the correlated BPM Process instance ID to signal; otherwise it signals with no process ID which usually results in a new process starting.

