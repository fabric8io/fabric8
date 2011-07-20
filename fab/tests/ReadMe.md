Sample FABs
-----------

This project contains a number of example projects for testing and demonstration purposes.

* [fab-sample-camel-noshare](https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-noshare) - jar using non-shared Camel directly, via an OSGi Activator
* [fab-sample-camel-blueprint-share](https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-blueprint-share) - jar using OSGi Blueprint to work with Camel (where Camel must be installed first as a feature)
* [fab-sample-camel-velocity-noshare](https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-velocity-noshare) - jar using non-shared Camel and Velocity directly, via an OSGi Activator
* [fab-sample-camel-velocity-share](https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-velocity-share) - jar using vanilla shared Camel and Velocity, via an OSGi Activator using META-INF/services discovery (setting the context ClassLoader in the Activator)
* [fab-sample-camel-velocity-require-bundle](https://github.com/fusesource/fabric/tree/master/fab/tests/fab-sample-camel-velocity-require-bundle) - as above but using Require-Bundle rather than Import-Package headers