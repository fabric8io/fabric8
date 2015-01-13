### Known issues about using fabric8

### Using captial letters in Karaf container name

Fabric8 does not support using captial letters in the `karaf.name` property in the `etc\system.properties`. If you change the value, then only use lower case letters.

#### I cannot start fabric8 on Windows

There is a known issue with Java and Windows when using IP6 capable network. You may see errors such as ``java.net.SocketException: Permission denied: no further information``. To resolve this set the ``KARAF_OPTS`` to the following in the ``bin/setenv.bat`` file.

    KARAF_OPTS="-Djava.net.preferIPv4Stack=true"

For more details see the [IP6 Java network guide](http://docs.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/) and this [knowledgebase solution](https://access.redhat.com/site/solutions/757533).