### Branding

The branding plugin applies an extra branding stylesheet depending on the version and type of server hawtio is running in.

The plugin checks the JVM's system properties using the following values:

* **propertiesToCheck** - What system properties the plugin should look for
* **wantedStrings** - If the plugin finds a matching system property then the plugin will see if that system property contains any of these strings.
