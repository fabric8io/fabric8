Building JBoss Fuse
===================

There are three main build targets associated with corresponding maven profiles

* fab: Fuse Fabric
* amq: Fuse A-MQ
* esb: Fuse ESB
* release: All of the above

Examples
--------

Build Fuse Fabric and run the associated smoke tests

    > mvn clean install
    
Build Fuse A-MQ and run the associated tests

    > mvn -Pamq clean install
    
Build Fuse ESB and run the associated tests

    > mvn -Pesb clean install
    
Build all modules and run the associated smoke tests

    > mvn -Prelease clean install
    
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

    > mvn -Prelease -Dts.all clean install
    
Build Fuse Fabric and skip the smoke tests

    > mvn -Dts.skip.smoke clean install
    
    