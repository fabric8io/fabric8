Hawt App
========

The Hawt App [maven](http://maven.apache.org) based build plugin makes it easy to create and 
launch simple java apps that use a flat classpath and a class with a
main.  It handles creating a `tar.gz` archive that contains all your app's 
runtime dependencies in the lib directory and creates a `bin/run` script that handles setting 
up your classpath in the right order and executing the process java.

Produced Artifacts
------------------

Using the build goal of this plugin will create the following:

* an unpacked assembly of the app in the `target/hawt-app` directory.
* a tar.gz archive of that assembly directory at: `target/${project.artifactId}-${project.version}-app.tar.gz` 

If you want to test out launching your app, just  execute the `target/hawt-app/bin/run.sh` script.

Usage
-------

You can use it on any maven module which contains a class that can be run from the CLI.  You just need to let it know
which main class to use.  For example:

    mvn package io.fabric8:hawt-app-maven-plugin:2.2.96:build -Dhawt-app.javaMainClass=org.apache.camel.cdi.Main
    
(replace the version number with the current version). To create the app as part of you default build for the module, 
add the a plugin configuration similar to the following in your maven module:

    <plugin>
      <groupId>io.fabric8</groupId>
      <artifactId>hawt-app-maven-plugin</artifactId>
      <version>2.2.96</version>
      <executions>
        <execution>
          <goals>
            <goal>build</goal>
          </goals>
          <phase>package</phase>
        </execution>
      </executions>
      <configuration>
        <javaMainClass>org.apache.camel.cdi.Main</javaMainClass>
      </configuration>
    </plugin>

Plugin Configuration Options
----------------------------

The following table contains the configuration properties you can set either in the plugin configuration or via a command line Maven property to adjust the results of the built application archive.

Name | Maven Property | Description 
-----| -------------- | -----------
assembly | hawt-app.assembly | Directory where the application assembly will be created. **Default:** *${project.build.directory}/hawt-app*
archive | hawt-app.archive | Archive file that will be created. **Default:** *${project.build.directory}/${project.artifactId}-${project.version}-app.tar.gz*
archiveClassifier | hawt-app.archiveClassifier | The GAV classifier that will be assigned to the archive. **Default:** *app*
archivePrefix | hawt-app.archivePrefix | the path prefixed on the files within the archive. **Default:** *${project.artifactId}-${project.version}-app/*
javaMainClass | hawt-app.javaMainClass | The main class that will be executed by the launch process.
source | hawt-app.source | If this directory exists, then it's contents are used to augment the contents of the application assembly. For example it could be used to add an etc/defaults script to setup environment variables. **Default:** *${basedir}/src/main/hawt-app*

Env Configuration Options
-------------------------

There are several environment variables that can be set before running the `bin/run` script to customize your app's startup.  

Environment Variable | Description
-------------------- | -----------
JAVA_APP_DIR | Directory holding the application (default: parent directory of the run script)
JAVA_OPTIONS | Options that will be passed to the JVM.  Use it to set options like the max JVM memory (-Xmx1G).
JAVA_ENABLE_DEBUG | If set to true, then enables JVM debugging  
JAVA_DEBUG_PORT | Port used for debugging (default: 5005)
JAVA_AGENT | Set this to pass any JVM agent arguments for stuff like profilers
JAVA_MAIN_ARGS | Arguments that will be passed to you application's main method.  **Default:** the arguments passed to the `bin/run` script.
JAVA_MAIN_CLASS | The main class to use if not configured within the plugin

Runtime Defaults Configuration
------------------------------

If an `${APP_HOME}/bin/setenv.sh` file exists it will be sourced in by the run script and you can use this to set
all the environment variables if you wish.  You can additionally also modify the values of the following variables 
if you want to change the defaults.

Environment Variable | Description
-------------------- | -----------
JAVA_MAIN_CLASS | The main class that will be executed. If given this willoverrite the configuration property `javaMainClass`
JAVA_CLASSPATH | The classpath of the java application to use. This will override the automatically determined classpath.

