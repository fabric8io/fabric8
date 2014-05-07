### Change Log

#### 1.2.1

* New [Maven plugin](http://hawt.io/maven/) for running hawtio in your maven project; running Camel, Spring, Blueprint or tests.
* New plugins:
  * API for viewing APIs from [Apache CXF](http://cxf.apache.org/) endpoints; currently only usable in a Fuse Fabric
  * IDE for generating links to open files in your IDE; currently IDEA the only one supported so far ;)
  * JUnit for viewing/running test cases
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

