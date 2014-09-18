## Requirements

Fabric8 makes it easy to add requirements on Profiles. For example that a service _foo_ requires at least 2 instances and a maximum of 5 and that it depends on a profile _bar_ to be running.

Once requirements are defined the tooling can report the health of the system and make it easy to automatically or manually scale your system to the right scale; using whatever container providers you want whether [Child](http://fabric8.io/gitbook/childContainers.html), [Docker](http://fabric8.io/gitbook/docker.html), [SSH](http://fabric8.io/gitbook/sshContainers.html), [Cloud](http://fabric8.io/gitbook/cloudContainers.html) or [OpenShift](http://fabric8.io/gitbook/openshift.html).

### Defining requirements

To define requirements using the web console, go into the **Runtime / Profiles** tab and you will see the current profiles running and any requirements in the _Target_ column.

Click on the _Target_ column to get a dialog to let you add/remove/edit the requirements.

From the command line shell you can use the **profile-scale** command to scale up or down the requirements for a profile.

e.g. to require 2 more instance of profile _foo_ to be running type:

    profile-scale foo 2

Or you can specify the exact number of instances (minimum and/or maximum) via the **require-profile-set** command

    require-profile-set --minimum 2 --maximum 5 mq-default

### Viewing the current requirements

In the web console you will see the requirements on the **Runtime / Profiles** tab. Also any requirements are reflected on the **Health** tab in the web console.

From the command line shell type the **fabric:status** command:

    fabric:status

Which should report on the health (100% indicates full health of each profile requirements).

You can view the full requirements details via:

    requirements-list

### Importing and exporting

You can use the **requirements-export** command to export the current requirements as a JSON file to the file system

    requirements-export /tmp/requirements.json

You can then edit them and them import them back again later or import them into a different fabric instance

    requirements-import /tmp/requirements.json

The **requirements-import** command can also taka URL as the command line argument; so you can host this file on a wiki, website or maven repository etc.

#### Using the REST API

You can also use the REST API to GET or POST requirements as JSON.

To find the REST API URL type:

    fabric:info

The default API is at [http://localhost:8181/api/fabric8/](http://localhost:8181/api/fabric8/) though this depends on your fabric.

For example the following curl command line will post an example set of requirements:

    curl -X POST -H "Content-type: application/json" -d '{"profileRequirements":[{"profile":"mq-default","minimumInstances":1,"maximumInstances":5},{"profile":"quickstarts-karaf-camel.amq","minimumInstances":1,"dependentProfiles":["mq-default"]}],"version":"1.0"}' http://localhost:8181/api/fabric8/requirements

Then the REST endpoint to GET or POST the fabric requirements is something like [http://localhost:8181/api/fabric8/requirements](http://localhost:8181/api/fabric8/requirements).

You can also navigate around the REST API following the links to find a version, a profile and then its requirements. e.g. the link for the mq-default profile for version 1.0 is something like [http://localhost:8181/api/fabric8/version/1.0/profile/mq-default/requirements](http://localhost:8181/api/fabric8/version/1.0/profile/mq-default/requirements)


### Auto Scaling

To enable the Auto Scaler just add the **autoscale** profile to the root container; or enable the following environment variable before you start the fabric process:

    export FABRIC8_PROFILES=autoscale

Once the **autoscale** profile is running internally a container is elected leader; which then watches for changes to the requirements in the Fabric configuration; when that changes the master then creates the necessary containers to match the requirements.

#### Automatically scaling up and down

The auto scaler will ensure that there's at least the minimum number of instances running of each profile you specify requirements for (starting them in dependency order).

To scale down the number of instances of a profile, just set the maximum instances count; if there are too many running the auto scaler will stop the correct number of containers.

#### Specifying which hosts to use with SSH

If you want to automatically provision and scale your profiles onto a bunch of machines via the [SSH Container Provider](http://fabric8.io/gitbook/sshContainers.html) you will need to specify the host requirements in the JSON file for the requirements.

There are [some examples](https://github.com/fabric8io/fabric8-devops/tree/master/autoscaler) of this together with mechanisms to test this out on your laptop such as with [vagrant scripts](https://github.com/fabric8io/fabric8-devops/tree/master/vagrant).

For example [this JSON demonstrates how to specify the hosts to be used to auto scale](https://github.com/fabric8io/fabric8-devops/blob/master/autoscaler/ssh-mq-demo.json#L25).

You can try out this example via the following command line:

    requirements-import https://raw.githubusercontent.com/fabric8io/fabric8-devops/master/autoscaler/ssh-mq-demo.json

### SLA based Auto Scaling with RHQ

When using Fabric8 and [RHQ](http://rhq.jboss.org/) together; all profiles metrics are automatically aggregated together globally. So you can then create RHQ alerts based on SLA or other metrics to decide when to scale up or down the number of instances of a Profile.

The RHQ alert can then use the above profile-scale operation (which is available on the [scaleProfile() method on the FabricManagerMBean](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-api/src/main/java/io/fabric8/api/jmx/FabricManagerMBean.java#L284) to scale up or down the profile requirements.
