## Local Cassandra

This profile lets you create a number of Cassandra containers on your laptop that form a logical cluster.

Under the covers this profile uses a new local IP address for each container; such as 127.0.0.1, 127.0.0.2, 127.0.0.3 etc.

**Note** to be able to use this profile to create more than one container you need to run the following command to add an IP address alias. (e.g. on OS X type):

    sudo ifconfig lo0 alias 127.0.0.2 up
    sudo ifconfig lo0 alias 127.0.0.3 up

Now you should be able to create 3 cassandra containers which should listen on 127.0.0.1, 127.0.0.2, 127.0.0.3.
