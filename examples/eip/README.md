# Content-Based Router

## Overview
This example demonstrates how to combine multiple EIPs in Camel to solve integration problems.

In this example, an orders file containing several orders for zoos around the world is being sent to us.  We first want to
make sure we retain a copy of the original file (using the Wiretap EIP), but afterwards, we want want to split the file up in
separate orders (using the Splitter EIP) and store the orders in separate directories by geographical region as well as
filter out the large orders (> 100 animals).

## What You Will Learn
In studying this example you will learn:

* how to define a Camel route using the Blueprint XML syntax
* how to build and deploy a Fuse Application Bundle (FAB) in Fuse ESB
* how to combine multiple Enterprise Integration Patterns to create an integration solution
* how to use direct: endpoint to link multiple smaller routes together

## Prerequisites
Before building and running this example you need:

* Maven 3.0.3 or higher
* JDK 1.6
* Fuse ESB Enterprise 7

## Building the Example
To build the example:

1. Change your working directory to the examples/eip directory
1. Run `mvn clean install` to build the example

## Running the Example
To run the example:

1. Start Fuse ESB Enterprise 7 by running bin/fuseesb (on Linux) or bin\fuseesb.bat (on Windows)
1. In the Fuse ESB console, enter the following command: `osgi:install -s fab:mvn:org.fusesource.examples/eip/${project.version}`
1. As soon as the Camel route has been started, you will see a directory work/cbr/input in your Fuse ESB installation
1. Copy the file you find in this example's src/test/data directory to the newly created work/eip/input directory
1. Wait a few moment and you will find multiple files organised by geographical region under work/eip/output
        2012_0003.xml and 2012_0005.xml in work/eip/output/AMER
        2012_0020.xml in work/eip/output/APAC
        2012_0001.xml, 2012_0002.xml and 2012_0004.xml in work/eip/output/EMEA
1. Use `log:display` to check out the business logging
        [main]    Processing orders.xml
        [wiretap]  Archiving orders.xml
        [splitter] Shipping order 2012_0001 to region EMEA
        [splitter] Shipping order 2012_0002 to region EMEA
        [filter]   Order 2012_0002 is an order for more than 100 animals
        ...

## More information
For more information see:
* http://www.eaipatterns.com/RecipientList.html for the Recipient List EIP
* http://www.eaipatterns.com/WireTap.html for the Wire Tap EIP
* http://www.eaipatterns.com/Filter.html for the Message Filter EIP
* http://www.eaipatterns.com/Sequencer.html for the Splitter EIP