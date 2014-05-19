## Fabric CXF Demo

This is a client/server example using Fabric and Apache CXF.

The client is a Java based client that uses Fabric CXF to discover where the CXF webservices is running in the fabric,
and then use a randome loadbalancing to access the services. 

This client is a standalone Java application (with a main method).

The Client uses the fabric-cxf API to connect to the fabric (using ZooKeeper) and use the Fabric load balancer feature with Apache CXF to load balance between the CXF web services that runs in the fabric servers. The fabric is elastic, so you can start/stop/move/scale up and down the CXF web services, and the Fabric load balancer will dynamic adapt to these changes.


### Installing

Install the server profile in a container.

### Running the client standalone

You can run the client using Apache Maven from the command line, using

    cd fabric/fabric-examples/fabric-cxf-demo/cxf-client
	mvn compile exec:java

### Running the client using fabric

You can install the client profile in a container.

### What happens

The client will connect to Fabric and use the Fabric load balancer feature in Apache CXF to discover where the CXF web services
are running in fabric, and connect and call these CXF services using a random load balancing strategy.

The client does 10 calls, with 5 second interval between, and outputs to System.out.

### Known Issues

You may need to **restart** the server if it fails to startup the first time.
The client cannot yet be installed as a profile in Fabric (currently not fully supported yet in fabric)
