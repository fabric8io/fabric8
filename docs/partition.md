## Partition

The Fabric8 Partition provides the ability of defining a task, that can be "partitioned" and distributed to fabric containers.
Each task can be associated with multiple work items (aka partitions) and those are distributed to the containers.

Some examples that make use of this functionality are:
i) Distribute raw camel routes in blueprint xml format to containers (see example-camel-partition.routes).
ii) Generate camel routes based on a template + json parameters and distribute them to containers (see example-camel-partition.json).

### Terminology
* **Task** A unit of work that can be split into individual work items that can be distributed to multiple containers.
* **Balancing Policy** A configured policy that specifies the way that the work items will be distributed to containers (currently only supported implementation is even distribution).
* **Work Item** A portion of the work that is distributed to the container.
* **Work Item Repository** A repository that contains work items. There are currently two implementations: i) ZooKeeper based and ii) Profile based.
* **Task Coordiantor** Joins a master election process. The master listens to the configured **Work Item Repository** for changes and distributes items according to the configured **Balancing Policy**.
* **Task Handler** A component that handles assignment of work items. The task handler passes the items assigned/removed to the **Worker** (see below).
* **Worker** An interface that describes how the container should handle when added/removed a work items. The worker type is configurable for each task.


## What does a work item look like?
A work item can be anything, that we want to distributed to containers. It can be a any file like a jar, an xml file, a json file.
As long as there is a worker that can handle the work item, the work item can be anything you like.

Inside Fabric8 the work item is represented by a unique id, by a location and a Map object *(if work item is a json file, it will get parsed into the map)*.

In the example-camel-partition.routes the work items are raw xml files containing camel blueprint routes.
In the example-camel-partition.json the work items are json blobs, which are used to render camel routes from a template.


## Creating Tasks
A task can be defined by providing a configuration pid that uses teh io.fabric8.partition factoryPid.

### Required configuration
* **id** A unique id that specifies the task.
* **balancingPolicy.target** The name of the balancing policy (e.g. even). It is passed as an LDAP filter (e.g. (type=even)).
* **workItemRepositoryFactory.target** The type of the work item repository (supported values: zookeeper, profile). It is passed as an LDAP filter (e.g. (type=zokeeper) or (type=profile)).
* **workItemPath** The path that contains the work items.
* **worker.target** The type of worker to use. It is passed as an LDAP filter (e.g. (type=profile-template)).

Note, that the balancing policy, the work item repository and the worker type are looked up from the Service Registry, using the specified value as a filter.

## Using fabric-partition to distribute dynamic profiles
As mentioned above, fabric-partition module provides a Worker implementation that creates profiles "on the fly" based on the assigned work items and a profile.
There are two example profiles that demonstrate this feature:

### example-camel-partition.routes
This profile can be used to automatically distribute raw xml routes to containers that are assigned this profile.
The structure of the profile looks like:

    example-camel-partition.routes
        |
        +-routes    (work item path)
        |   |
        |   +->route1.xml (example work item)
        |   +->route2.xml
        |   +->route3.xml
        |   +->route4.xml
        |
        +-io.fabric8.agent.properties
        +-io.fabric8.partition-example.properties (task configuration)

The io.fabric8.partition-example.properties defines a task as follows:

    id=example
    workItemRepositoryFactory.target=(type=git)
    workItemPath=profile:example-camel-partition.routes/routes
    balancingPolicy.target=(type=even)
    worker.target=(type=profile-template)
    templateProfile=example-camel-template.routes

In the configuration above the workItemPath represents the location where the work items (raw xml files) are stored. The workerType represents the type of Worker to use (in this case its the profile template worker). Last the templateProfile which is a configuration parameter specific to the worker, is the template profile to be used.
This profile also contains a folder, with 4 simple camel routes (these are the actual work items).

The configuration specifies a template profile which is example-camel-template.routes:

     example-camel-template.routes
         |
         +-io.fabric8.agent.properties.mvel

 this profile just contains a simple template configuration that just defines that for each work item should be deployed as a raw blueprint xml file (as specified by io.fabric8.agent.properties.mvel):

    bundle.profile.camel-@{item.id}=blueprint:profile:@{item.location}

### example-camel-partition.json
Similar to the previous example, this profile will distribute camel routes to containers. In this case the camel routes are not static, but are rendered using a template + parameters.
The parameters are in the json format. These json blobs will play the role of the work item. Each containers will be assigned one or more json blobs and for each assigned json, the template will be rendered and deployed to the container

The structure of the profile looks like:

    example-camel-partition.json
        |
        +-items    (work item path)
        |   |
        |   +->1 (example work item. It's a simple json that looks like: {inUri : direct:start1}
        |   +->2 {inUri : direct:start2}
        |   +->3 {inUri : direct:start3}
        |   +->4 {inUri : direct:start4}
        |
        +-io.fabric8.agent.properties
        +-io.fabric8.partition-example.properties (task configuration)

The io.fabric8.partition-example.properties defines a task as follows:

    id=example
    workItemRepositoryFactory.target=(type=git)
    workItemPath=profile:example-camel-partition.json/items
    balancingPolicy.target=(type=even)
    worker.target=(type=profile-template)
    templateProfile=example-camel-template.json

In the configuration above the workItemPath represents the location where the work items (json files) are stored. The workerType represents the type of Worker to use (in this case its the profile template worker). Last the templateProfile which is a configuration parameter specific to the worker, is the template profile to be used.
This profile also contains a folder, with 4 simple camel routes (these are the actual work items).

The configuration specifies a template profile which is example-camel-template.json:

     example-camel-template.json
         |
         +-camel__item.data.it__.xml.mvel
         +-io.fabric8.agent.properties.mvel

The camel__item.data.it__.xml.mvel is the route template:

        <camelContext id="camel-@{item.id}" trace="false" xmlns="http://camel.apache.org/schema/blueprint">
            <route id="route-@{item.id}">
                <from uri="@{item.data.inUri}" />  <!-- Everything under item.data are pulled in from the json blob -->
                <to uri="log:requests" />
            </route>
        </camelContext>

This template is rendered into a plain xml and is stored inside the profile using camel<item.id>.xml as a file name.
The rendered route is then deployed as the io.fabric8.agent.properties.mvel specifies:

    bundle.profile.camel-@{item.id}=blueprint:profile:camel@{item.id}.xml

**The Template Profile Worker**
In both of the examples above we used the template profile worker. The template profile workers role is to look up the template profile and render it using the assigned work items.
The worker is using mvel templates, so it goes through the template profile resources and for each resource that is an mvel template (identified by its extension) it renders the template and stores it to a new profile that is unique per task and container.
The name of the profile will always be <template profile name>.<container name>. Non mvel resources will be just copied as is to the new profile.
Last the new profile is assigned to the container. If the rendered profile is empty the profile will get deleted and removed from the container.

**Other Template Profiles**
You could use your own template profiles to do things like distributing features to containers:

    template
    |
    +----------> io.fabric8.agent.properties.mvel

The io.fabric8.agent.properties.mvel could look like this:

    feature.@{name}=@{name}

So give a partition item that contains the following json:

    { "name" : "cool-feature" }

The worker will render the file as follows:

    feature.cool-feature=cool-feature

Then it will create a new profile and assign it to local container.

*Note:* If you need to also have the resource names under the profile template, you can use in the file name a placeholder surrounded by double underscores, e.g: camel-__id__.xml.

##The profile work item repository

The examples above made use of the Profile Work Item Repository. This repository can lookup for work items to distribute under the profile, using the Profile URL Handler as a work item path.
You can then add / remove work items, by adding or removing resources from the profile path.

##The zookeeper work item repository

An alternative to the profile work item repository, is the zookeeper work item repository. When using it, you specify a zookeeper path that will contain work items.
You can then add/remove znodes to that path, that will contain the work items. For example if you specify the workItemPath /fabric/partition/example. You can add workitems using the shell like:

    zk:create /fabric/partition/example/1 "{ \"inUri\" : \"direct:in1\" }"
    zk:create /fabric/partition/example/2 "{ \"inUri\" : \"direct:in2\" }"


## Implementing custom Workers
In most cases the user will want to implement his own workers. Implementing one is as trivial as implementing the following methods.

    String getType();

    void assign(TaskContext context, Set<WorkItem> items);

    void release(TaskContext context, Set<WorkItem> items);

The getType() method should return a string, which can be used for looking up the listener (used in the workerType property).
The assign/release methods can be used to implement the behavior of the worker when items are added/removed.
The argument context represents the task and it encapsulates the task configuration.
The items argument is a representation of the items, which contains a unique identifier for the item, the location of the item and a java.util.Map which can contain additional data if work item is in json format.

