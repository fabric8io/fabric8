## Camel CXF Demo

This example demonstrates using Camel CXF as a SOAP web service.

The example is implemented using code-first development, where the web service is coded in the
java source file **io.fabric8.examples.camelcxf.Greeter**.

### Deploying the example to fabric8

This example can be deployed to fabric8, from the command line using

    mvn fabric8:deploy

And then create a new container using the camel.cxf.demo profile.

### What happens

When the example runs, the web service is exposed at **http://localhost:9090/greeter/**.

You can also see the web service in the [API](#/fabric/api) browser in the web console.

