### Change Log

#### 1.2.2

* Added welcome page to aid first time users, and being able to easily dismiss the welcome page on startup.
* Added preference to configure the order/enabling of the plugins in the navigation bar, and to select a plugin as the default on startup.
* Added support for Apache Tomcat security using the conf/tomcat-users.xml file as user database.
* Added [quartz](http://hawt.io/plugins/quartz/) plugin to manage quartz schedulers.
* Allow to configure the HTTP session timeout used by hawtio. hawtio now uses the default timeout of the web container, instead of hardcoded value of 900 seconds.
* The [JMX](http://hawt.io/plugins/jmx/) plugin can now edit JMX attributes.
* the [osgi](http://hawt.io/plugins/osgi/) plugin now supports OSGi Config Admin and uses OSGi MetaType metadata for generating nicer forms (if the io.fabric8/fabric-core bundle is deployed which implements an MBean for introspecting the OSGi MetaType).

* Fixes [these 75 issues and enhancements](https://github.com/hawtio/hawtio/issues?milestone=8&state=closed)

#### 1.2.1

* New [Maven plugin](http://hawt.io/maven/) for running hawtio in your maven project; running Camel, Spring, Blueprint or tests.
* New plugins:
  * [JUnit](http://hawt.io/plugins/junit/) for viewing/running test cases
  * [API](http://hawt.io/plugins/api/) for viewing APIs from [Apache CXF](http://cxf.apache.org/) endpoints; currently only usable in a Fuse Fabric
  * [IDE](http://hawt.io/plugins/ide/) for generating links to open files in your IDE; currently IDEA the only one supported so far ;)
  * Site plugin for using hawtio to view and host your project website
* Improved the camel editor with a new properties panel on the right

* Fixes [these 51 issues and enhancements](https://github.com/hawtio/hawtio/issues?milestone=3&state=closed)

#### 1.2.0

* Connectivity
  * New _JVMs_ tab lets you connect to remote JVMs on your local machine; which if a JVM does not have jolokia installed it will install it on the fly. (Requires tools.jar in the classpath)
  * New _Connect_ tab to connect to a remote JVM running jolokia (and its now been removed from the Preferences page)
* ActiveMQ gets huge improvements in its tooling
  * we can more easily page through messages on a queue
  * move messages from one queue to another
  * delete messages
  * retry messages on a DLQ (in 5.9.x of ActiveMQ onwards)
  * purge queues
* Camel
  * Neater message tracing; letting you zoom into a message and step through the messages with video player controls
  * Can now forward messages on any browseable camel enpdoint to any other Camel endpoints
* Fabric
  * Redesigned fabric view allows quick access to versions, profiles and containers, mass-assignment/removal of profiles to containers
  * Easier management of features deployed in a profile via the "Edit Features" button.
  * Several properties now editable on container detail view such as local/public IP and hostname
* General
  * Secured embedded jolokia, performs authentication/authorization via JAAS
  * New login page
  * Redesigned help pages
* Tons more stuff we probably forgot to list here but is mentioned in [the issues](https://github.com/hawtio/hawtio/issues?milestone=4&state=closed) :)

* Fixes [these 407 issues and enhancements](https://github.com/hawtio/hawtio/issues?milestone=4&state=closed)

#### 1.1

* Added the following new plugins:

  * [forms](https://github.com/hawtio/hawtio/blob/master/hawtio-web/src/main/webapp/app/forms/doc/developer.md) a developer plugin for automatically creating tables and forms from json-schema models 
  * [infinispan](http://hawt.io/plugins/infinispan/) for viewing metrics for your Infinispan caches or using the CLI to query or update them
  * [jclouds](http://hawt.io/plugins/jclouds/) to help make your cloud hawt
  * [maven](http://hawt.io/plugins/maven/) to let you search maven repositories, find versions, view source or javadoc
  * [tree](https://github.com/hawtio/hawtio/blob/master/hawtio-web/src/main/webapp/app/tree/doc/developer.md) a developer plugin to make it easier to work with trees

* Added a new real time Camel profile view and the first version of a web based wiki based camel editor along with improvements to the diagram rendering

* Added more flexible documentation system so that plugins are now self documenting for users and developers

* Fixes [these 80 issues and enhancements](https://github.com/hawtio/hawtio/issues?milestone=2&state=closed)


#### 1.0

* First main release of hawtio with [lots of hawt plugins](http://hawt.io/plugins/index.html).
* Fixes [these 74 issues and enhancements](https://github.com/hawtio/hawtio/issues?milestone=1&state=closed)

#### In Progress (1.3)

* [threads](https://github.com/hawtio/hawtio/tree/master/hawtio-web/src/main/webapp/app/threads) plugin to monitor JVM thread usage and status.
* Moved java code from hawtio-web into hawtio-system
* Upgraded to TypeScript 0.9.5 which is faster
* Clicking a line in the log plugin now shows a detail dialog with much more details.
* Breadcrumb navigation in Camel plugin to make it easier and faster to switch between CamelContext and routes in the selected view.
