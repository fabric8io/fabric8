# Synopsis

The `stream-log` module a simple program for use in conjunction with Apache HTTPD's piped logfile feature.  It takes
log events from an Apache web server and streams them to an ActiveMQ messaging broker.  This is typically done
to aggregate log event from multiple web servers and process them in realtime.

Example Apache configuration:

    CustomLog "| java -jar stream-log-1.1-SNAPSHOT.jar --broker tcp://localhost:61616 --destination topic:http.log" common


# TODO:

  * Get the --compress true option working
  * Validate that event loss does not occur when apache is shutdown.
  * Validate that if JMS flow control kicks in, you don't buffer event without bound.
  * Optimize the uber jar size
  * Include dependencies to use fabric: discovery
  