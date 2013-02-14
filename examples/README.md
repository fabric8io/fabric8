# JBoss Fuse Examples

The examples included with JBoss Fuse are intended to demonstrate how to implement common use cases. Each example includes a `README.md` file that explains:

* the use case shown in the example
* the prerequisites for building and running the example
* directions for building and running the example

All of the code included with the examples are thoroughly documented by comments in the code.

## Routing Examples
* `cbr` - demonstrates how to create a content-based router
* `eip` - demonstrates how to link several Enterprise Integration Patterns to solve an integration problem
* `errors` - demonstrates how to handle exceptions in a route
* 'jms' - demonstrates how to create a content-based router using a JMS based router

## Services Examples
* `rest` - demonstrates how to build a RESTful Web service using JAX-RS
* `secure-rest` - demonstrates how to secure a RESTful Web service using JAAS
* `secure-soap`- demonstrates how to use WS-Security to secure a JAX-WS Web service
* `soap` - demonstrates how to build a SOAP/HTTP Web service using JAX-WS

# FUSE IDE

You can import the JBoss Fuse examples into Fuse IDE. Fuse IDE allows you to view graphical representations of the Camel routes and to perform run time testing in a distributed environment.

To import the examples into Fuse IDE:

1. From the menu, select File -> Import...
2. In the dialog, select Maven -> Existing Maven Projects and click Next.
3. In the next dialog, select your ESB installation's examples directory as the root directory.
4. Eclipse will now scan for projects and list them in the 'Projects:' section - once that process is done, click 'Finish'.
