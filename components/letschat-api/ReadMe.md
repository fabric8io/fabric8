Fabric8 LetsChat API
====================

This library provides a JAXRS 2.0 based Java client API to [Let's Chat](http://sdelements.github.io/lets-chat/) chat service

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

             <dependency>
                 <groupId>io.fabric8</groupId>
                 <artifactId>letschat-api</artifactId>
                 <version>2.2.96</version>
             </dependency>

### Environment variables

The following environment variables are required:

* LETSCHAT_TOKEN the token used to authenticate with letschat

#### Optional environment variables

* LETSCHAT_USERNAME login user name
* LETSCHAT_PASSWORD password for basic authentication (token is the preferred way with Let's Chat)


### Building

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

Then you should be able to build it via:

    cd components/letschat-api
    mvn clean test-compile
