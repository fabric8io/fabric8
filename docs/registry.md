## Registry

Fabric8 uses [Apache ZooKeeper](http://zookeeper.apache.org/), which is highly reliable distributed coordination service,
as its registry to store the cluster configuration and node registration.

ZooKeeper is designed with consistency and high availability across data centers in mind while also protecting against network splits by using a quorum of ZooKeeper servers. e.g. you may run 3 or 5 ZooKeeper servers and so long as you have quorum running (2 or 3 servers respectively) you are reliable and are not in a network split.

Conceptually Fabric has 2 registries:

* **Configuration Registry** which is the logical configuration of your fabric and typically contains no physical machine information; its your logical configuration.
* **Runtime Registry** which contains details of how many machines are actually running, their physical location details and what services they are implementing.

### Initializing the registry

There are currently two ways of initializing the fabric registry:

      fabric:create

### Registry structure

The structure of the registry is a tree like structure similar to a filesystem. Each node of the tree *(will be called znode)* can hold both data and have children.

Here is how the structure of the registry looks like:

        fabric
            |
            +----registry (runtime registry)
            |        |
            |        +----containers
            |                 |
            |                 +----root
            |
            +----configs (configuration registry)
                     |
                     +----versions
                     |        |
                     |        +----1.0
                     |               |
                     |               +----profiles
                     |                        |
                     |                        +----default
                     |
                     +----containers



### Making the registry highly available

Having a single container hosting the registry, doesn't provide high availability. In order to have a highly available registry, we need to add more containers as part of it. The common term used to describe the group of servers that are forming the registry is ensemble.
Fabric allows you to dynamically add or remove containers from the ensemble, by using the [command line shells in Karaf](commands/commands.html).

#### Adding containers to the ensemble

Assuming that we have created a fabric cluster, with a single registry container as described above and that we have already added a couple of containers join the fabric cluster.
The list of fabric containers could look like this:

        [id]                           [version] [alive] [profiles]                     [provision status]
        root*                          1.0       true    fabric, fabric-ensemble-0000-1
        container1                     1.0       true    default                        success
        container2                     1.0       true    default                        success

You could then add container1 and container2 as part of the ensemble using the **fabric:ensemble-add** command from the shell:

       fabric:ensemble-add container1 container2

You can watch the following clip, which demonstrates the above. It actually starts a fresh fabric, install fabric into 2 additional containers in the local network *(from scratch using just the shell)* and finally adds the containers to the ensemble.


<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/qCujpN4hPgY?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/qCujpN4hPgY?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>



### Accessing the registry at the zookeeper level

Even though fabric does provide a rich tool set for accessing the registry *(both configuration & runtime)*, working with profiles and managing the containers, there might be cases where someone, needs to access the registry at the zookeeper level.
To cover those needs fabric provides a set of commands.

To install the fabric zookeeper commands you need to install the fabric-zookeeper-commands feature.

        features:install fabric-zookeeper-commands

Please note, that if you want to use that feature inside a managed container, you will have to use the profiles, instead.
The commands that are provided are:

* **zk:list** *<path>* Lists the znodes under the specified path.

* **zk:get** *<znode>* Returns the value of the specified znode.

* **zk:set** *<znode>* *<value>* Sets the value of the specified znode.

* **zk:create** *<znode>* Create a new znode.

* **zk:delete** *<znode>* Deletes a znode.

#### Modifying the runtime configuration of a container using the zookeeper commands

In the following video you can see how to install the zookeeper commands and how you can use them to inspect and edit the registry.
The example demonstrates how you can use the zookeeper commands to manually assign an extra ip to an existing fabric container.

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/ZiFbFMTMyjc?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/ZiFbFMTMyjc?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>




