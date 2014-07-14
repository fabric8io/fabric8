## Requirements

Fabric8 makes it easy to add requirements on a Profile. For example that a service _foo_ requires at least 2 instances and that it depends on a profile _bar_ to be running.

Once requirements are defined the tooling can report the health of the system and make it easy to automatically or manually scale your system.

### Defining requirements

To define requirements using the web console, go into the **Runtime / Profiles** tab and you will see the current profiles running and any requirements in the _Target_ column.

Click on the _Target_ column to get a dialog to let you add/remove/edit the requirements.

From the command line shell you can use the **profile-scale** command to scale up or down the requirements for a profile.

e.g. to require 2 more instance of profile _foo_ to be running type:

    profile-scale foo 2

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

### SLA based Auto Scaling with JON

When using Fabric8 and RHQ together; all profiles metrics are automatically aggregated together globally. So you can then create RHQ alerts based on SLA or other metrics to decide when to scale up or down the number of instances of a Profile.

The JON alert can then use the above profile-scale operation (which is available on the [scaleProfile() method on the FabricManagerMBean](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric-core/src/main/java/io/fabric8/api/jmx/FabricManagerMBean.java#L223-223) to scale up or down the profile requirements.
