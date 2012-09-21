Fuse MQ Enterprise
------------------

Configuration
-------------
The default broker is defined in .${fileSeparator}etc${fileSeparator}org.fusesource.mq.fabric.server-default.cfg
The xml configuration is .${fileSeparator}etc${fileSeparator}activemq.xml

Security
--------
Before starting Fuse MQ, you need to set at least one valid user in .${fileSeparator}etc${fileSeparator}users.properties
You must also make sure that appropriate properties in .${fileSeparator}etc${fileSeparator}system.properties have valid values

These properies include:
* activemq.jmx.user, activemq.jmx.password
* webconsole.jmx.user, webconsole.jmx.password
* webconsole.jms.user, webconsole.jms.password


Quick Start
-----------
To start Fuse MQ Enterprise in the background, type:

    ${startCommand}

Note: Be sure to use the appropriate username and password in the following examples
To display the log using the remote console, type:

    ${clientCommand} -u fusemq - p fusemq log:display

To display the current broker statistics using the remote console, type:
    
    ${clientCommand} -u fusemq -p fusemq activemq:bstat

To validate the installation with a simple JMS producer and consumer, type:

    java -jar lib${fileSeparator}mq-client.jar producer --user fusemq --password fusemq
    java -jar lib${fileSeparator}mq-client.jar consumer --user fusemq --password fusemq

View the webconsole at http://localhost:8181/activemqweb

Documentation
-------------
You can find documentation online at:
http://fusesource.com/documentation/fuse-mq-enterprise-documentation

