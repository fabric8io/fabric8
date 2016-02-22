Fabric8 Git Repo API
==================

This library provides a JAXRS 2.0 based Java client API to git based repositories such as <a href="http://gogs.io/">gogs</a> or <a href="http://github.com/">github</a>

### Add it to your Maven pom.xml

To be able to use the Java code in your [Apache Maven](http://maven.apache.org/) based project add this into your pom.xml

             <dependency>
                 <groupId>io.fabric8</groupId>
                 <artifactId>gitrepo-api</artifactId>
                 <version>2.2.96</version>
             </dependency>

### Building

If you clone the source code:

    git clone https://github.com/fabric8io/fabric8.git
    cd fabric8

Then you should be able to build it via:

    cd components/gitrepo-api
    mvn clean test-compile
