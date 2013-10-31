# Fabric Partition

Fabric partition provides the ability of defining a task, that can be "partiotioned" and distributed to fabric containers.
Each task can be associated with multiple work items (aka partitions) and those are distributed to the containers.

An example use of this feature is generating profiles dynamically based on a template and distributing them accross containers (see below).

###Terminology
* **Task** The definition of the work.
* **Partition** A portion of the work (work unit) that is distributed to the container. It is represented in json format.
* **PartitionListener** An interface that describes how the container should handle when added/removed a partition. The master listens for changes in the partitions and calls the BalancingPolicy if needed. Both master and slave react to partition assignment and notify the PartitionListener.
* **TaskManager** A service that is responsible for the coordination of a task. For each taks its responsible for electing a master, that will take care of the partition assignment.

##Creating Tasks
Fabric provides a ManagedServiceFactory that is responsible for creating a TaskManager for each configured task.
To create a new TaskManager the user needs add a configuration with the factoryPid org.fusesource.fabric.partition, e.g:

    fabric:profile-edit --resource org.fusesource.fabric.partition-example.properties <target profile>

The command above will open a text editor where the user can define the task configuration:

    id=example
    partitions.path=/fabric/partition/example
    balancing.policy=even
    worker.type=profie-template
    task.definition=mytemplateprofile

In this configuration, the id uniquely identifies the configuration.
The balancing policy describes how the partitions should be balanced. Fabric provides out of the box the "even" balancing policy, but the user can implement his own and export them as an OSGi service using the service property "type" to distinguish.
The key partitions.path defines the path in the registry where the partitions are stored.
The worker.type defines the PartitionListener implementation. The implementation is looked up in the OSGi service reference, using the property "type" as a filter.
The task.definition is defines the task. It can be any value the PartitionListener can understand. In the current example it specifies a template profile. The profile-template PartitionListener knows how to handle it.

##Using fabric-partition to distribute dynamic profiles

As mentioned above, fabric-partition module provides an implementation of the PartitionListener that creates profiles "on the fly" based on a template + partition data.

**The template profile**
The template profile is a profile which contains mvel templates as resources. The ProfilePartitionListener will search for resources with the mvel extentition and will render them using the key/value pairs stored inside the assigned partition.

**Example Template Profile**

    template
    |
    +----------> org.fusesource.fabric.agent.properties.mvel

The org.fusesource.fabric.agent.properties.mvel could look like this:

    feature.@{name}=@{name}

So give a partition item that contains the following json:

    { "name" : "cool-feature" }

The ProfilePartitionListener will render the file as follows:

    feature.cool-feature=cool-feature

Then it will create a new profile and assign it to local container.

*Note:* If you need to also have the resource names under the profile temlated, you can use in the file name a placeholder surrounded by double underscores, e.g: camel-__id__.xml.

##The camel template example
Out of the box the example-camel-partition & example-camel-template profiles are provided. The example-camel-partition is a template profile that contains a templated camel route.
The route contains a variable consumer endpoint which sends messages to log. This allows you to generate profiles with different consumer endpoint and distribute them to containers.

The example-camel-partition contains the configuration of the task. Each container that uses this profile will register a ProfilePartitionListener and will wait for partition assignment.
The task configuration looks like this:

    id=example
    task.definition=example-camel-template
    partitions.path=/fabric/partition/example
    balancing.policy=even
    worker.type=profile-template

You can create partition using the zookeeper commands like this:

    zk:create /fabric/partition/example/1 "{ \"inUri\" : \"direct:in1\" }"
    zk:create /fabric/partition/example/2 "{ \"inUri\" : \"direct:in2\" }"

This will result in generating the example-camel-template-1 and example-camel-template-2 prpfiles and distribute them evenly to containers running the example-camel-partition profile.

##Implementing custom partition listeners
In most cases the user will want to implement his own partition listener. Implementing one is as trivial as implementing the following methods.

    String getType();

    void start(String taskId, String taskDefinition, Set<Partition> partitions);

    void stop(String taskId, String taskDefinition, Set<Partition> partitions);

The getType() method should return a string, which can be used for looking up the listener (used in the worker.type property).
The start/stop methods can be used to implement the behavior of the listener when partition items are added/removed. The arguemnts taskId and taskDefintion take values fromt he task configuration. The partions argument is a representation of the partition, which contains a unique identifier for the partition and a java.util.Map created from the json data of the Partition stored in the registry.

