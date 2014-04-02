Modules
===================

By default Fabric8 builds the set of modules required by the karaf distribution and runs the associated integration tests.
There are three main build targets associated with corresponding maven profiles

* tomcat: Fabric8 on Tomcat 
* wildfly: Fabric8 on WildFly
* all: All available modules

Build examples are below

Committers
----------

Be sure to check out the [committer instructions](http://174.129.32.31:8080/) on how to fork this repo and submit Pull Requests

Building Fabric8
============

Build Fabric8 and run the associated smoke tests

    > mvn clean install
    
Build Fabric8 Tomcat and run the associated tests

    > mvn clean install -Ptomcat
    
Build Fabric8 WildFly and run the associated tests

    > mvn clean install -Pwildfly
    
Build Fabric8 using latest hawtio Snapshot and run the associated tests

    > mvn -Phawtio-snapshot clean install

Note, to avoid getting prompted for a gpg key add **-Dgpg.skip=true**

Quick Builds
==========

You can do quick builds by appending `-DskipTests`

Build Fabric8 and skip tests

    > mvn clean install -DskipTests

Build fabric8 with all modules and skip tests

Test Profiles
==========

Fabric8 tests are seperated in serveral dedicated tests profiles

* ts.smoke:   Smoke tests
* ts.basic:   Basic integration tests
* ts.all:     All of the above

Examples
--------

Build Fabric8 and run the smoke and basic integration tests

    > mvn clean install -Dts.basic
    
Build Fabric8 and run all tests

    > mvn clean install -Dts.all
    
Build fabric8 with all modules and run all tests

    > mvn clean install -Pall -Dts.all
    