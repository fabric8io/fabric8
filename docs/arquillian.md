## Arquillian

We use [Arquillian](http://arquillian.org/) as the primary container based testing framework; allowing us to write tests once and then reuse them when running embedded or remote containers.

For example the [fabric-itests-common module](https://github.com/fabric8io/fabric8/tree/master/itests/common) defines a bunch of common arquillian based integration tests using fabric8, such as, [FabricCreateCommandTest.java](https://github.com/fabric8io/fabric8/blob/master/itests/common/src/main/java/io/fabric8/itests/common/FabricCreateCommandTest.java#L61) which spin up a fabric; they can be tested with an embedded fabric codebase or ran in a karaf, tomcat or wildlfy container etc.

### Remote Auto Scale Tests

Its good to be able to test the binary distritution formats; such as

* the zip / tarballs
* [docker](http://docker.io/) container images
* [OpenShift](https://www.openshift.com/) cartridge

Also we have the [Auto Scaling](http://fabric8.io/gitbook/requirements.html) capability, that given a set of requirements we can auto scale the required containers to be created in the right order on the various different container providers: [child](http://fabric8.io/gitbook/cloudContainers.html), [ssh](http://fabric8.io/gitbook/sshContainers.html), [docker](http://fabric8.io/gitbook/docker.html), [openshift](http://fabric8.io/gitbook/openshift.html) or [cloud](http://fabric8.io/gitbook/cloudContainers.html).

So we have a set of remove container plugins for arquillian that take the requirements and auto scale them.

For example the test case [AutoScaleSingleMessageBrokerTest.java](https://github.com/fabric8io/fabric8/blob/master/itests/autoscale/autoscale-itests-common/src/main/java/io/fabric8/itests/autoscale/AutoScaleSingleMessageBrokerTest.java#L42-42) uses one of the available arquillian plugins to create the initial fabric; then it defines the [requirements it needs](http://fabric8.io/gitbook/requirements.html) (i.e. how many instances of each profile and how they should be placed onto hosts) and then asserts that they can be properly provisioned within the given amount of time.



