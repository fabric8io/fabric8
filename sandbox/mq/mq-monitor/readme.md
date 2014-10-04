# Synopsis

The `mq-monitor` is simple program that monitors JMX metrics of an ActiveMQ broker and logs alerts when 
it detects possible error conditions.

Usage:

    java -jar mq-monitor-1.1-SNAPSHOT.jar --user admin --password activemq --jmx service:jmx:rmi:///jndi/rmi://127.0.0.1:11099/jmxrmi

