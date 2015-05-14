Building Fabric8
============

Due to certain plugin dependencies, Building Fabric8
requires a version of Maven >= 3.2.5.

First of all, the Fabric8 build process may need, 
more memory than the default allocated
to the maven process. Therefore, ensure to set the 
MAVEN_OPTS system property with the following settings
before starting

    > MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"

Build Fabric8 and run the associated tests

    > mvn clean install

Quick Builds
==========

You can do quick builds by appending `-DskipTests`

Build Fabric8 and skip tests

    > mvn clean install -DskipTests

Build fabric8 with all modules and skip tests

Test Profiles
==========

Fabric8 tests are seperated in serveral dedicated tests profiles

* ts.all:    Includes additional testing

Examples
--------
  
Build Fabric8 and run regular tests

    > mvn clean install

Build Fabric8 and run all tests

    > mvn clean install -Dts.all
    

Building including SNAPSHOTS
============================

Fabric8 allows using SNAPSHOT builds of hawtio and/or Camel using Maven profiles

* camelSnapshot
* hawtioSnapshot

For example to build Fabric8 using latest hawtio Snapshot and run the associated tests

    > mvn -Phawtio-snapshot clean install

Note, to avoid getting prompted for a gpg key add **-Dgpg.skip=true**


License check
=============

The source code uses the license header from the file ```fabric-license-header.txt``` in the root directory.

You can check for missing licenses in the source code, by running the following goal from the root directory. Notice this will check all the source code:

    > mvn license:check com.mycila:license-maven-plugin:2.6:check -Dlicense.header=fabric-license-header.txt

And from any sub module, you need to refer to the license file using a relative path:

```
   > cd components
   > mvn license:check com.mycila:license-maven-plugin:2.6:check -Dlicense.header=../fabric-license-header.txt 
```

You can update the license headers in the source code using the ```format``` goal, for example:

    > mvn license:format com.mycila:license-maven-plugin:2.6:check -Dlicense.header=../fabric-license-header.txt 


GitBook
=======

The documentation is compiled into a book using [GitBook](https://github.com/GitbookIO/gitbook).

First install gitbook using npm

    npm install -g gitbook

And then install the anchor plugin

    sudo npm install -g gitbook-plugin-anchors    

Note on osx you may need to run these commands with `sudo`

And then build the book locally using

    cd docs
    gitbook serve ./

And access the book from a web browser at

    http://localhost:4000

To add new sections into the gitbook, ecit the `docs/SUMMARY.md` file.
