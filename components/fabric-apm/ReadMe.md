## Fabric8 APM - Application Performance Management Module

A Java Agent that can be attached to a JVM at startup, or attached to a running process.

Currently it instruments methods to record timings around methods and populates mbeans in the JMX domain **io.fabric8.apmagent**

### Trying it out using Maven

Type the following command to build and run the test application:

    mvn install exec:exec

You should be able to connect hawtio now to http://127.0.0.1:8778/jolokia/ to view the metrics from the JMX plugin in the **io.fabric8.apmagent** domain
Or connect directly to the running JVM using jconsole and see the metrics in the **io.fabric8.apmagent** domain


#### Trying it out using shell scripts

Also there are two shell scripts in `src/test/bin` you can use for testing:

1. From the top directory, run `src/test/bin/apmTest.sh` -
   This will start a small test application with the APM Java Agent already attached
1. For the brave, find the process id for a running JVM (e.g. ActiveMQ) -
   Then run `src/test/bin/apmAgent <process id>` to attach the APM agent to the running process
