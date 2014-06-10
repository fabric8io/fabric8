## Beginner QuickStarts

This folder containers various beginner quickstart examples to help you get started with the various aspects of Fabric

First time users of Fabric is recommended to try the following quickstarts (see further below for more details)

1. `camel.log` or `camel.log.wiki` - a very simple Camel application 
1. `camel.cbr` or `camel.cbr.wiki` - a simple Camel application using the well known Content Based Router EIP

These two quickstarts is describer a bit more below (and by clicking the links):

* [camel.log](/fabric/profiles/example/quickstarts/beginner/camel.log.profile) is a very simple Camel application that uses a timer to trigger a message every 5th second and writes that to the server log.
* [camel.log.wiki](/fabric/profiles/example/quickstarts/beginner/camel.log.wiki.profile) is the same example as [camel.log](/fabric/profiles/example/quickstarts/beginner/camel.log.profile) but this time the routes are stored in an <a fabric-version-link="/camel/canvas/fabric/profiles/example/quickstarts/beginner/camel.log.wiki.profile/camel-log.xml">XML file inside the wiki</a> so you can edit it via the browser and use <a href="/fabric/profiles/docs/fabric/rollingUpgrade.md">rolling upgrades</a> to update it and roll forward/backward changes to containers without having to release any Java artifacts.
* [camel.cbr](/fabric/profiles/example/quickstarts/beginner/camel.cbr.profile) is a Camel application that uses a Content Based Router to route incoming files based on their content, to different destinations.
* [camel.cbr.wiki](/fabric/profiles/example/quickstarts/beginner/camel.cbr.wiki.profile) is the same example as [camel.cbr](/fabric/profiles/example/quickstarts/beginner/camel.cbr.profile) but this time the routes are stored in an <a fabric-version-link="/camel/canvas/fabric/profiles/example/quickstarts/beginner/camel.log.wiki.profile/camel-log.xml">XML file inside the wiki</a> so you can edit it via the browser and use <a href="/fabric/profiles/docs/fabric/rollingUpgrade.md">rolling upgrades</a> to update it and roll forward/backward changes to containers without having to release any Java artifacts.

There is a number of other beginner quickstarts which is recommended to explore and try:

* [camel.eips](/fabric/profiles/example/quickstarts/beginner/camel.eips.profile) to learn more about other Enterprise Integration Patterns (EIP) supported by Apache Camel.
* [camel.errorhandler](/fabric/profiles/example/quickstarts/beginner/camel.errorhandler.profile) quickstart introduces how you can handle errors with Apace Camel.

For more information click the quickstart of choice, for instructions how to install and try the quickstart. 
