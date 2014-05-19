## Fabric CXF Demo

This is a client/server example using Fabric and Apache CXF.

The client is a Java based client that uses Fabric CXF to discover where the CXF webservices is running in the fabric,
and then use a randome loadbalancing to access the services. 

### Installing

Install the server and/or client profile in separate containers.

### Known Issues

You may need to **restart** the server if it fails to startup the first time.

