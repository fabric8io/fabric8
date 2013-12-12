Building JBoss Fuse
===================

There are three main build targets associated with corresponding maven profiles

* amq: Fuse A-MQ
* esb: Fuse ESB
* all: All available modules

Build examples are below

Committers
----------

Be sure to check out the [committer instructions](http://174.129.32.31:8080/) on how to fork this repo and submit Pull Requests

Examples
--------

Build Fuse Fabric and run the associated smoke tests

    > mvn clean install
    
Build Fuse A-MQ and run the associated tests

    > mvn -Pamq clean install
    
Build Fuse ESB and run the associated tests

    > mvn -Pesb clean install
    
Build Fuse Fabric using last Hawtio Snapshot and run the associated tests

    > mvn -Phawtio-snapshot clean install
    
Build all modules and run the associated smoke tests

    > mvn -Pall clean install

Note, to avoid getting prompted for a gpg key add **-Dgpg.skip=true**

Test Profiles
-------------

Fuse Fabric tests are seperated in serveral dedicated tests profiles

* ts.smoke:   Smoke tests
* ts.basic:   Basic integration tests
* ts.wildfly: WildFly integration tests
* ts.all:     All of the above

Examples
--------

Build Fuse Fabric and run the smoke and basic integration tests

    > mvn -Dts.basic clean install
    
Build Fuse Fabric and run all tests

    > mvn -Dts.all clean install
    
Build all modules and run all tests

    > mvn -Pall -Dts.all clean install
    
Build Fuse Fabric and skip the smoke tests

    > mvn -Dts.skip.smoke clean install
    
