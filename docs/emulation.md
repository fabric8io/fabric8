## Emulation Layer

This layer supports the Kubernetes REST APIs on non-linux platforms (as some folks using Java middleware don't use linux). The use case is specifically to just support running Java application servers on non-linux; we have no plans to try emulate the whole docker ecosystem.

### Providing a Zip Process alternative to Docker images

To be able to start/stop containers without using Docker (so we can start/stop things like Karaf, Tomcat, WildFly on non-linux) we will need to create a **Zip Process** which is a ZIP file with shell scripts to start/stop/restart.

So that rather than linux LXC containers, we'd map image names to Zip Process files; unzip files into folders on disk and run shell scripts to start/stop/restart processes.

These Zip Processes images would be very limited; offer none of the isolation and quota behaviour of LXC containers but they would provide a similar abstraction to docker so we could manage Java application servers on any platform.

### Implementation Options

The exact details are being worked out; there are a few options we could consider:

#### Reusing OpenShift / Kubernetes code

* compile the openshift/kubernetes go-lang code to each platform and use it everywhere (like docker and etcd already work on other platforms)
  * docker is supported on most platforms so we could use that too
* when folks want to really use the underlying host system to provision Java application servers we would map image names to _Zip Process_ images

#### Hybrid approach

We could keep the OpenShift / Kubernetes code in a VM in terms of etcd, the master, the kube-proxy etc. Then port the kubelet (starting/stopping containers) to use Docker and / or Zip Processes

#### Writing a pure Java implementation

We could refactor the Java code from fabric8 1.x which is a conceptual kubernetes like thing and port it to implement the Kubernetes REST API.

We could then reuse the existing Docker support or support Zip Process (by porting the 1.x code for Process Manager / Process Container)