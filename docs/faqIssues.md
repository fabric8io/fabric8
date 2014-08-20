### Known issues about using fabric8

#### Why does the welcome screen print two times?

When starting fabric8 using `bin\fabric8` the welcome screen may be printed two times. This will be resolved in a future release of fabric8, by upgrading to Apache Karaf 2.4.0 when it becomes available.

### Using captial letters in Karaf container name

Fabric8 does not support using captial letters in the `karaf.name` property in the `etc\system.properties`. If you change the value, then only use lower case letters.

