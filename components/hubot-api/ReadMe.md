Fabric8 Hubot API
=================

This library provides a JAXRS 2.0 based Java client API for working with the <a href="http://hubot.github.com/">Hubot chat bot</a> that lets you send notifications to chat systems such as:

* Lets Chat
* IRC
* Slack
* HipChat
* Campfire

### Using the API

We recommend you inject a HubotNotifier into your code via either CDI or Spring:

    @Inject HubotNotifier hubot;
    
    ...
    
    hubot.notify("#myroom", "hello @fabric8, this site looks cool: http://fabric8.io/");

The room names can be anything at all really; usually they start with a \# character.

We currently use the default of using **\#fabric8_default** as the room name for the default namespace but different naming conventions can be used to refer to different kubernetes namespaces.


### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

             <dependency>
                 <groupId>io.fabric8</groupId>
                 <artifactId>hubot-api</artifactId>
                 <version>2.2.96</version>
             </dependency>

### Building

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

Then you should be able to build it via:

    cd components/hubot-api
    mvn clean test-compile
