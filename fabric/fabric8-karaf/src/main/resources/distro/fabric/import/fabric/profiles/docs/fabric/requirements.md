## Requirements

Fabric8 makes it easy to add requirements on a Profile. For example that a service _foo_ requires at least 2 instances and that it depends on a profile _bar_ to be running. Once requirements are defined the tooling can report the health of the system and make it easy to auto-scale or manually scale your system.

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

### Auto Scaling

When using Fabric8 on OpenShift we have an out of the box auto-scaler which uses the requirements to automatically create the required number of instances of each profile.

Under the covers a container is elected leader; which then watches for changes to the requirements in the Fabric configuration; when that changes the master then creates the necessary OpenShift cartridges to match the requirements.

### SLA based Auto Scaling with JON

When using Fabric and JON together; all profiles metrics are automatically aggregated together globally. So you can then create JON alerts based on SLA or other metrics to decide when to scale up or down the number of instances of a Profile.

The JON alert can then use the above profile-scale operation (which is available on the [scaleProfile() method on the FabricManagerMBean](https://github.com/jboss-fuse/fuse/blob/master/fabric/fabric-core/src/main/java/io/fabric8/api/jmx/FabricManagerMBean.java#L223-223) to scale up or down the profile requirements.
