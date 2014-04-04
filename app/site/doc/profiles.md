## Profiles

A profile is a description of how a logical group of containers needs to be provisioned. It contains a list of:

* System properties
* OSGi Configuration Admin PIDs
* Maven artifact repositories
* Bundles
* Karaf feature repositories
* Features

and also defines the OSGi framework that is going to be used.

Each profile can have none, one or more parents, and this allows allows you to have profile hierarchies and a container can be assigned one or more profiles.
Profiles are also version, which allows you to keep different versions of each profile and then upgrade or rollback containers by changing the version of the profiles they use.

### Profile hierarchies
It is quite often that multiple profiles share similar bits of configuration. Its quite common different application to use common frameworks libraries etc. Defining everything from group up for each profile can be a real pain and is not that easy to maintain.
To avoid having duplicate configuration across profiles and reduce the required maintenance, Fabric uses a hierarchical model for profiles, which allows you to build a generic profile which contains common configuration and then inherit the common bits.

The section below describes the profiles that are shipped with Fabric out of the box and are a good example of how profile hierarchies work.

### Out of the box profiles
Fabric provides a rich set of profiles *"out of the box" that can be used as the basic building blocks for definining your own profiles. The most important profiles are:

* **default** The default profile defines the all the basic stuff that fabric needs to run. For example it defines the *fabric-agent* feature, the fabric registry url & the list of maven repositories that can be used to download artifacts from.
* **karaf** It is a child of **default** (so it doesn't need to define the same things again. It also defines the karaf feature repositories, that can be used for defining any karaf feature.
* **camel** It is a child of **karaf**. It also defines the camel feature repositories and some core camel features such as *camel-core* & *camel-blueprint*. Any profile for describing camel application is suggested to inherit this one.
* **cxf** It is a child of **karaf**. It also defines the cxf feature repositories and some core cxf features. It is intended to be the parent of any profile that describes a cxf application.
* **mq-base** A profile that inherit the **karaf** profile and defines the  *mq-fabric* feature
* **mq** It is a child of the **mq-base** profile and it also provides a fuse mq broker configuration.
* **esb** It is a child of **camel**,**mq** & more profiles and also defines the *Fuse ESB* feature repository.

### Changing the profile of a container
At any give time you are able to change on of more of the profiles that are assigned to a container. You can use the [fabric:container-change-profile](commands/fabric-container-change-profile.html) command as shown below:

      fabric:container-change-profile mycontainer myprofile

The command above will assign the **myprofile** profile to **mycontainer**. All profiles previously assigned to the container will be removed. You can also specify multiple profiles to the container:

       fabric:container-change-profile mycontainer myprofile myotherprofile

### Creating and editing profiles

To see the list of available profiles you can use the **fabric:profile-list**:

        fabric:profile-list

The command will display all the profiles and also display their parents and the number of containers that currently make use of each profile:


        [id]                                     [# containers] [parents]
        activemq-client                          0              default
        aws-ec2                                  0              cloud
        camel                                    0              karaf
        camel-jms                                0              camel, activemq-client
        cloud                                    0              karaf
        cloudservers-uk                          0              cloud
        cloudservers-us                          0              cloud
        cxf                                      0              karaf
        default                                  0
        dosgi                                    0              karaf
        example-camel                            0              karaf
        example-cxf                              0              cxf
        example-mq                               0              example-mq-base
        example-mq-base                          0              karaf
        example-mq-cluster                       0              example-mq-base
        fabric                                   1              karaf
        fmc                                      0              default
        hadoop                                   0              karaf
        hadoop-datanode                          0              hadoop
        hadoop-namenode                          0              hadoop
        hawtio                                   0              default
        insight                                  0              default
        insight-hdfs                             0              insight
        jboss-fuse-full                          0              jboss-fuse-medium
        jboss-fuse-medium                        0              jboss-fuse-minimal, mq
        jboss-fuse-minimal                       0              karaf, camel
        karaf                                    0              default
        mq                                       0              mq-base
        mq-base                                  0              karaf
        nmr                                      0              karaf

To see exactly what a profile defines you can use the **fabric:profile-display** command. For example let's take a look of what is defined in the camel profile.

        fabric:profile-display camel

This command will display all information available for the camel profile:

        Profile id: camel
        Version   : 1.0
        Parents   : karaf
        Associated Containers :

        Container settings
        ----------------------------
        Repositories :
	        mvn:org.apache.camel.karaf/apache-camel/2.9.0.fuse-7-061/xml/features

        Features :
	        camel-blueprint/2.9.0.fuse-7-061
	        fabric-camel/99-master-SNAPSHOT
	        camel-core/2.9.0.fuse-7-061

Of course this command does not display what is inherited from the parents of the profile *(in this example the karaf profile)*. To unfold the profile hierarchy and also see the inherited configuration you can use the **--overlay** option:

        fabric:profile-display --overlay camel

#### Creating a new profile
In order to create a new profile that will describe how your application should be provisioned, you can use the [fabric:profile-create](commands/fabric-profile-create.html) command.

        fabric:profile-create myprofile

To specify one ore more parents to the profile you can use the **--parents** option:

        fabric:profile-create --parents camel myprofile

As soon as the profile is created you can modify the profile, using the commands that are described in the following sections of the document.

#### Adding or removing a feature to a profile
In order to edit one of the existing profile you can use the [fabric:profile-edit](commands/fabric-profile-edit.html) command.

In this example I will use the command to add the *camel-jclouds* feature to the **camel** profile.

        fabric:profile-edit --features camel-jclouds camel

After the command I can display again the profile and see how the camel profile looks like now. You should now be able to see the *camel-jclouds* feature in the list of features of the **camel** profile.

        Features :
        	camel-jclouds
        	camel-blueprint/2.9.0.fuse-7-061
        	camel-core/2.9.0.fuse-7-061
        	fabric-camel/99-master-SNAPSHOT

If you want to remove a feature from the profile you can make use of the **--delete** option. So, if gor example you need to remove the *camel-jclouds* feature:

        fabric:profile-edit --delete --features camel-jclouds camel

#### Modifying a configuration pid in a profile
A more complex example is when you need to modify the a configuration pid of a profile. A configuration pid is actually a list of key value pairs. So to edit or add a new key value pair to a specific pid you can use the **-pid** and specify the pid and key value in the following format *pid/key=value*.
In the following example, I will modify the *io.fabric8.agent* pid and change the maven repository list. The default profile should contain a section like this:

        Agent Properties :
        	  org.ops4j.pax.url.mvn.repositories = 	http://repo1.maven.org/maven2,
        		 https://repo.fusesource.com/nexus/content/repositories/releases,
        		 https://repo.fusesource.com/nexus/content/groups/ea,
        		 http://repository.springsource.com/maven/bundles/release,
        		 http://repository.springsource.com/maven/bundles/external,
        		 https://oss.sonatype.org/content/groups/scala-tools

Let's see how we can change the Agent Properties section *(the agent properties is represented by the io.fabric8.agent pid that was mentioned above)*:

        		 fabric:profile-edit --pid io.fabric8.agent/org.ops4j.pax.url.mvn.repositories=http://repositorymanager.mylocalnetwork.net default

Now the [fabric:profile-edit](commands/fabric-profile-edit.html) command for the **default** profile should be similar to:

                 Agent Properties :
                 	  org.ops4j.pax.url.mvn.repositories = http://repositorymanager.mylocalnetwork.net

### Profile Editor

The profile edit command is quite flexible, however in some cases, you would really prefer a more traditional way of editing. Mainly becasue:

* You want to perform many changes at once
* It feels more natural

This is why fabric provides a profile editor embedded to your shell. You can use it, like:

      fabric:profile-edit default (with no options)

![Opening the profile inside the editor](/images/fabric/profile-edit.png)

The editor supports the basic editor functionality like undo, redo, forward/backward search, highlighting etc. What you are actually editing is the content of the io.fabric8.agent pid.
But you can use it to also edit any other pid or resource in your profile. For example to edit the io.fabric8.maven pid of the fabric profile, you can simply:

      fabric:profile-edit --pid io.fabric8.maven fabric

![Editing a pid](/images/fabric/profile-edit-pid.png)

A pretty similar approach applies to any resouce under the profile. For example, to edit the broker.xml of the mq-base profile:

      fabric:profile-edit --resource broker.xml mq-base

![Editing a pid](/images/fabric/profile-edit-resource.png)


### Profile versions
Every profile has at least one version. When assigning a profile to a container, you actually assign both the profile and the version. The [fabric-agent](fabric-agent.html), will choose the defined version and retrieve all the information provided by the specific version of the profile.

Any change to a profile, will take immediate effect. This means that if there are containers that are assigned the version of a profile that was just modify will pick up the change immediately.
So it is recommended, to create a new version of a profile whenever you need to make changes and then assign the new version to the container. This allows you to complete your changes, test them and rollback to the previous version if you encounter issues.

#### Create a new version
You are able to create a new version using the [fabric:version-create](commands/fabric-version-create.html). The default version is 1.0 so let's create 1.1.

        fabric:version-create 1.1

Once the version has been created an instance of each profile has been created for the new version. To be more precise a copy of the latest version of each profile has been added to the new version.
Now you can display or modify the 1.1 version of each profile. Let's display the **camel** profile:

        fabric:profile-display --version 1.1 camel

The output will be identical to the 1.0 version of the profile, since we haven't applied any change to the profile yet.

But how do we modify a specific version of profile?

All you need to do is to specify the version right after the profile argument. For example let's add the *camel-jclouds* component to version 1.1 of the **camel** profile:

        fabric:profile-edit --features camel-jclouds camel 1.1

Please note, that this will not affect any of your existing container, not until you upgrade them to the 1.1 version.

#### Rolling upgrades & rollbacks
Fabric provides commands for upgrading *(increasing the version)* and rolling back *(decreasing the version)* of the profiles assigned to a container. In order to upgrade a single container to the 1.1 version that we created in the previous section you can use the [fabric:container-upgrade](commands/fabric-container-upgrade.html). For example:

        fabric:container-upgrade 1.1 mycontainer

The command above will make make *mycontainer* to use the version 1.1 of all the profiles that have been currently assigned to it.

If for any reason you wish to rollback to the previous version, you can make use of the [fabric:container-rollback](commands/fabric-container-rollback.html) command.

        fabric:container-rollback 1.1 mycontainer

That doesn't look like rolling, does it. You are strongly recommended to test your changes on a single container, before applying the changes to the whole cluster. Applying an upgrade to all containers can be achieved using the **--all** option as demonstrated below.

          fabric:container-upgrade --all 1.1 mycontainer

#### Complete walk through

The following clip demonstrates most of the profile concepts & features described so far. It uses a small Fabric cluster of 5 containers on EC2, plus 1 Fabric registry.
It describe how to modify profiles and explains how to perform single and rolling container upgrades.

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/-2W5NwC2oAo?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/-2W5NwC2oAo?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>

#### Importing and exporting profiles
There are cases where you have put quite a lot of effort in creating the profiles that much your needs and you want to store them somewhere.
A good example is when you move from the development environment to the staging or the production environment. You simply just don't want to go over the process of creating the profiles again.
For such cases Fabric allows you export your profiles in text and also import them back. So you can safely store them or even import them to a version control system.

To export the Fabric profiles you can use the [fabric:export](commands/fabric-export.html)

         fabric:export

This command will export the whole registry to files. The default export location is the fabric/export folder under the karaf home directory. To change the default location you just need to specify the path as an argument:

         fabric:export /path/to/my/export/location

Of course the registry also contains runtime information which may be unneeded. You can choose initial znode from which the export will occur. For example to just keep the configuration data:

         fabric:export -p /fabric/configs /path/to/my/export/location

You also have the option to include or exclude part of the registry using regular expression.

In a similar way the import operation works. Please keep in mind that by default when creating a Fabric the [fabric:create](commands/fabric-create.html) command will import everything it finds in fabric/import under the karaf home folder.

        fabric:create

to specify an other folder for importing to the registry you can simply use the **--import-dir** option. For example:


        fabric:create --import-dir /path/to/my/import/location

Of course there are cases where you need to import data to the registry after the registry has been created. You can use the the [fabric:export](commands/fabric-export.html) as described below:

        fabric:import

All the arguments and options of the [fabric:export](commands/fabric-export.html) are also available to the [fabric:import](commands/fabric-import.html) command.

For example if you exported the registry starting from the /fabric/configs/ znode and want to import them back starting from the same znode *(this is what makes sense, when exporting starting from a specific znode to import back starting from the same)*:

      fabric:import -p /fabric/configs /path/to/my/import/location

