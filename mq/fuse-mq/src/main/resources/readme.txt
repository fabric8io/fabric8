Disclaimer
--------------------------------------------------------------------
This is beta software intended for testing and experimentation only.
It is not intended for production use. Please, do not use in a
production environment.
--------------------------------------------------------------------

Fuse MQ Enterprise
------------------

The default broker is defined in .${fileSeparator}etc${fileSeparator}org.fusesource.mq.fabric.server-default.cfg
The xml configuration in .${fileSeparator}etc${fileSeparator}activemq.xml

To start Fuse MQ Enterprise with a karaf console, type:

    ${startCommand}

To validate the installation with a simple JMS producer and consumer,
from another command window, type:

    java -jar lib${fileSeparator}mq-client.jar producer
    java -jar lib${fileSeparator}mq-client.jar consumer

View the webconsole at http://localhost:8181/activemqweb

