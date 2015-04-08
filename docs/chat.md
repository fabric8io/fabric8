## Chat

DevOps is all about culture and teams efficiently deploying and managing software. Using Chat software is a great way to help the cultural and social side of DevOps.

There are various types of Chat software like IRC, Slack, HipChat, Campfire and so forth.

In Fabric8 we recommend the use of [hubot](https://hubot.github.com/), an open source chat bot that works with most Chat software.

Fabric8 comes with a hubot [app](apps.html) and a notification engine to post [build completion](builds.html) events to a chat room (which defaults to one room per kubernetes namespace).

### How to run the Chat Apps in Fabric8

If you [installed Fabric8](openShiftDocker.md) using the kitchen sink option then you have all the apps running.

Otherwise if you are running Fabric8 with the [Fabric8 Console](console.html) then go to the **Apps** tab.

* click the **Run...** button and select the **hubot** app to and run it.
* click the **Run...** button and select the **hubot notifier** app to and run it.

A quick way to do the above is to type **hubot** into the filter box at the top and then just select all the apps it finds and then hit the **Run** button.

One you have completed the above you should see the **Chat** item on the navigation bar of the [Console](console.html)

### How to use Chat in Fabric8

Click the **Chat** item on the navigation bar of the [Console](console.html). By default you are then prompted to enter your nickname (which defaults to the user name you used to work with the git repositories on the **Repositories** tab.

You can also enter an IRC room name which defaults to **#fabric8-default** for the default namespace.
