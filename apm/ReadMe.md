APM - Application Performance Management Module

A Java Agent that can be attached to a JVM at startup, or attached to a running process
Currently it instruments methods to record timings around methods - and populates mbeans in the 
JMX domain io.fabric8.apmagent


Firstly build the project with mvn clean install

There are two shell scripts in src/test/bin you can use for testing:

1. From the top directory, run src/test/bin/apmTest.sh - this will start a small test application with the
APM Java Agent already attached

2. For the brave, find the process id for a running JVM (e.g. ActiveMQ)
Then run src/test/bin/apmAgent <process id> to attach the APM agent to the running process
