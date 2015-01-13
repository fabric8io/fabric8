# Example Camel AutoTest

This profile makes it easy to automatically test camel routes whenever the camel route or transformations are changed.

The idea is that you can add to the wiki test messages to be sent as inputs to routes (using a directory per CamelContext ID and route ID). The test messages are sent to the route whenever the route is redeployed (e.g. if you edit the camel route via the wiki, or update the version of a bundle etc).

This means as you change the profile with the route inside it, you can in a separate window browse the outputs of the sample messages on the different output endpoints.