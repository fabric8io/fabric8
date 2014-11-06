## Node

The node is an exectuable main in Java which has the following behviour:

* on startup it uses command line arguments / environment variables to connect to a ZooKeeper ensemble; or creates one on the fly
* it listens on a REST HTTP port (say 8181) and registers its own host/port into the /jube/nodes area of ZK
* a leader is elected which then takes over mapping resources to nodes

