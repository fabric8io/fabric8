# Synopsis

The `stream-log` module a simple application to stream log files to an ActiveMQ or FuseMQ message broker.  
It can be used in one of two ways.  You can have it stream rotating log files stored on your file system or
you can pipe it log data via standard input. This is typically done
to aggregate log event from multiple web servers and process them in realtime.

A common way to use it is to launch it via Apache HTTPD's piped logfile feature.

Example Apache configuration:

    CustomLog "| java -jar stream-log-99-master-SNAPSHOT.jar --broker tcp://localhost:61616 --destination topic:http.log" common

But if you want ensure no messages are ever lost even if the process dies, then you should have it read
the log data by tailing your log files.

    $ java -jar target/stream-log-99-master-SNAPSHOT.jar --help
    Overview:

      This application will streaming log data from standard input or by tailing
      rotating log files and efficiently send them to an ActiveMQ broker.

    Usage:

    To stream log data from standard input use:
      --broker <url> --destination <name> [batch-options]

    When data is streamed from standard input, the data is sent to the broker
    non persistently since the application would not be able to recover
    from failure if one did occur.

    To stream log data from rotating log files:
      --log-file <file> --position-file <file> --broker <url> --destination <name> [batch-options]

    When data is streamed from a log file, the data is sent persistently
    to the broker since if the application fails it can resume
    sending from where it last succeed since it's using a position
    file to track what has been successfully sent to the broker.

    Broker Options:
     --broker <url>
         The ActiveMQ broker URL to send events to. Example:
         "tcp://localhost:61616"
     --destination <name>
         The ActiveMQ destination name to send events to.  Example:
         "queue://test.log" or "topic://test.log"

    Log File Options:
     --log-file <file>
         The log file name pattern.  You can use printf format string
         to specify the rotating log file pattern.  For example:
         "/var/logs/apache/access.log.%d"  It will start streaming
         from log file 0 and continue looking for increasing log files
         to stream.  If you wish to start from a different log file
         or skip log files, manually edit the position file.
     --position-file <file>
         The position file is used to keep track of the
         last log file record which was successfully streamed
         to the broker.  If you restart the process streaming
         will resume from the position stored in the position file.
         The file format is "<file-number:<file-offset>" You
         can manually edit this file if you want to resume
         streaming from a different log file # or offset.

    Batch Options:
     --batch-size <bytes>
         The number bytes to batch up before sending log events to the broker.
         defaults to 65536.
     --batch-timeout <seconds>
         The maximum amount of time we spend trying to build a bigger batch.
         Defaults to 5.
     --compress <bool>
         Should the batch be compressed with Snappy?  Defaults to
         false.

    Other Options:
     --help
         Displays this help screen.

    Example Usage:

      --broker tcp://localhost:61616 --destination topic://foo.log \

