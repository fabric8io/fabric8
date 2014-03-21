Modules
===================

There are three main build targets associated with corresponding maven profiles

* amq: Fuse A-MQ (to be removed)
* esb: Fuse ESB  (to be removed)
* all: All available modules

Build examples are below

Committers
----------

Be sure to check out the [committer instructions](http://174.129.32.31:8080/) on how to fork this repo and submit Pull Requests

Building fabric8
============

Build fabric8 and run the associated smoke tests

    > mvn clean install
    
Build Fuse A-MQ  (to be removed) and run the associated tests

    > mvn -Pamq clean install
    
Build Fuse ESB  (to be removed) and run the associated tests

    > mvn -Pesb clean install
    
Build fabric8 using latest hawtio Snapshot and run the associated tests

    > mvn -Phawtio-snapshot clean install
    
Build all modules and run the associated smoke tests

    > mvn -Pall clean install

Note, to avoid getting prompted for a gpg key add **-Dgpg.skip=true**

Quick Builds
==========

You can do quick builds by appending `-Dtest=false`

Build fabric8 and skip tests

    > mvn clean install -Dtest=false



Test Profiles
==========

fabric8 tests are seperated in serveral dedicated tests profiles

* ts.smoke:   Smoke tests
* ts.basic:   Basic integration tests
* ts.wildfly: WildFly integration tests
* ts.all:     All of the above

Examples
--------

Build fabric8 and run the smoke and basic integration tests

    > mvn -Dts.basic clean install
    
Build fabric8and run all tests

    > mvn -Dts.all clean install
    
Build all modules and run all tests

    > mvn -Pall -Dts.all clean install
    
Build fabric8 and skip the smoke tests

    > mvn -Dts.skip.smoke clean install
    
