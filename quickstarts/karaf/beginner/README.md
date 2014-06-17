Beginner Quickstarts
====================

Welcome to the beginner quickstarts. These quickstarts demonstrates how to build Apache Camel integrations with fabric8.

The following two quickstarts is the _hello world_ beginner level examples, which we recommand first time users to try.

* [camel.log](/fabric/profiles/quickstarts/karaf/beginner/camel.log.profile) - is a very simple Camel application using a timer to trigger a message every 5th second which is then written to the server log.
* [camel.log.wiki](/fabric/profiles/quickstarts/karaf/beginner/camel.log.wiki.profile) - wiki based example of [camel.log](/fabric/profiles/quickstarts/karaf/beginner/camel.log.profile) where the Camel routes are stored in an <a fabric-version-link="/camel/canvas/fabric/profiles/quickstarts/karaf/beginner/camel.log.wiki.profile/camel-log.xml">XML file inside the wiki</a> so you can edit it via the browser and use <a href="/fabric/profiles/docs/fabric/rollingUpgrade.md">rolling upgrades</a> to update it and roll forward/backward changes to containers without having to release any Java artifacts.

The following quickstarts are also beginner examples that uses Apache Camel.

* [camel.cbr](/fabric/profiles/quickstarts/karaf/beginner/camel.cbr.profile) - is a small Camel application using the Content Based Router (one of the most common EIP pattern)
* [camel.eips](/fabric/profiles/quickstarts/karaf/beginner/camel.eips.profile) - demonstrates a number of other commonly used EIP patterns with Apache Camel.
* [camel.errorhandler](/fabric/profiles/quickstarts/karaf/beginner/camel.errorhandler.profile) - introduces to error handling with Camel, such as using redelivery and a Dead Letter Channel.

 