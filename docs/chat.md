## Chat

DevOps is all about culture and teams efficiently deploying and managing software. Using Chat software is a great way to help the cultural and social side of DevOps.

There are various types of Chat software like IRC, Slack, HipChat, Campfire and so forth.

In Fabric8 we recommend the use of [hubot](https://hubot.github.com/), an open source chat bot that works with most Chat software.

Fabric8 comes with a hubot [app](apps.html) and a notification engine to post [build completion](builds.html) events to a chat room (which defaults to one room per kubernetes namespace).

### Installation
 
To install this app please see the [Install Fabric8 on OpenShift Guide](fabric8OnOpenShift.html). Chat is included in the [Continuous Delivery](cdelivery.html), [iPaaS](ipaas.html) and [Kitchen Sink](fabric8OnOpenShift.html#kitchen-sink) apps.    

One you have completed the above you should see the **Chat** item on the navigation bar of the [Console](console.html)

### How to use Chat in Fabric8

Click the **Chat** item on the navigation bar of the [Console](console.html). By default you are then prompted to enter your nickname (which defaults to the user name you used to work with the git repositories on the **Repositories** tab.

You can also enter an IRC room name which defaults to **#fabric8-default** for the default namespace.
