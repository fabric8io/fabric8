## Cloud Containers

Fabric leverages [jclouds](http://www.jclouds.org/) in order to allow Fabric create new containers in public or private clouds.
The Fabric cloud container provider will allow you to create new compute instances in the cloud provider of your choice, perform firewall configuration, requirements installation & last but not list full container installation and automatic registration to the cluster.

### Requirements

The requirements for using this feature to create a container in a remote host are:

* **A valid account to a supported cloud provider**

The list of supported cloud providers can be found at [jclouds supported providers](http://jclouds.apache.org/reference/providers/).
**Important** The term **supported provider** does not refer to commerical support, its just an indication that there is an available implementation.

#### Additional requirement for hybrid clusters

A hybrid cluster is a cluster that is composed of containers running both in the premises and inside a public cloud provider. This special type of cluster may impose some additional requirements:

* **Guarantee that all containers will be able to connect the registry** *(refers to network connectivity)*

In order to satisfy this requirement, you will need to make sure that one of the following conditions are meet:

* **Fabric registry is running inside the public cloud**
* **Cloud and Local containers are part of a VPN**
* **Fabric registry is accessible from the internet** *(Not recommended)*

In the case that the Fabric registry is running the cloud, your local containers will have no problem accessing the registry as long as they are able to connect to the internet.
However, if the Fabric registry is running in the premises, the cloud containers, won't be able to have access to your premises. At least not unless you have the registry accessible from the internet or unless you setup a vpn.
Having the registry accessible from the internet is not really the best idea for the obvious reasons. Setting up a vpn sounds like a better idea.

The easiest approach is just to host the registry in the cloud & configure the firewall in the cloud accordingly to only allow access to containers from the premises. By default Fabric will configure the firewall for you. Below you can see how you can create an ensemble in the cloud *(host the registry in the cloud)*, there is also a small demonstration video covering that case.

### Preparation

Before you can make use of this feature you will need to obtain a valid *identity* and *credential* for your cloud provider. That is not necessarily the username and password you obtain upon registration with the provider. This is usually refers to the credentials that you get for using the service from an external api.
For example for Amazon EC2 these can be found at the [security credentials page](https://aws-portal.amazon.com/gp/aws/securityCredentials).

The next step is to make sure that the container you will be using will have all the required features installed. The core feature requirement is *fabric-jclouds* which will give you access to the cloud container provider.
Then you need to install the feature for your corresponding cloud provider.

        features:install fabric-jclouds

Note that if you are connected to a managed container the features command will not be available. In this case you will need to make sure that the feature above is part of the profile you are using.

#### Feature naming convention

The naming convention for the cloud provider features is jclouds-<provider id>, where provider id is as listed in [jclouds supported providers](http://www.jclouds.org/documentation/reference/supported-providers).

Some common feature names:

* **jclouds-aws-ec2** The feature for Amazon EC2
* **jclouds-cloudservers-us** The feature for Rackspace

        features:install jclouds-aws-ec2
        features:install jclouds-cloudservers-us

For those two cloud providers Fabric also provides profiles that will install all the required features, so you could just you the provided profiles out of the box.

### Registering a cloud provider

Once you have installed all the required feature, you need to register the cloud provider to fabric. The registration process acutally will store the provider credentials to the registry so that they can be used from any Fabric container.

The registration can be done with the use of fabric:cloud-service-add.

To register the Amazon EC2 provider:

        fabric:cloud-service-add aws-ec2 myidentity mycredential

To register the Rackspace provider

        fabric:cloud-service-add cloudservers-us myidentity mycredential


### Creating containers in the cloud

You can now use the *fabric:container-create-cloud-command* to create new Fabric containers in the cloud.

For example to create a container on Amazon EC2:

       fabric:container-create-cloud --provider aws-ec2 mycontainer

We still haven't mentioned anything about images. This is just because specifying an image is optional. By default Fabric will try to find an *Ubuntu* image for you. You can provide predicates for the *operating system* and/or the *version* of it.
For example to choose *Centos* instead of *Ubuntu* you could make use of the **--os-familu** option:

         fabric:container-create-cloud --provider aws-ec2 --os-family centos mycontainer

         fabric:container-create-cloud --provider aws-ec2 --os-family centos --os-version 5 mycontainer

The latest will try to find any centos version that contains 5 inside it.

Of course if you have need to specify the exact image, Fabric will allow you to using the **--image** option.

         fabric:container-create-cloud --provider aws-ec2 --image myimageid mycontainer

The command will display the creation status and also some usefull information, once the creation is complete:


        Looking up for compute service.
        Creating 1 nodes in the cloud. Using operating system: ubuntu. It may take a while ...
        Node fabric-f674a68f has been created.
        Configuring firewall.
        Installing fabric agent on container cloud. It may take a while...
        Overriding resolver to publichostname.
                          [id] [container]                    [public addresses]             [status]
          us-east-1/i-f674a68f cloud                          [23.20.114.82]                 success


#### Creating a new Fabric ensemble in the cloud

The cloud container provider not only allows you to create containers in the cloud, it also allows you to create a new ensemble in the cloud using the **--ensemble-server** option.
Here is a short clip that demonstrates how you can create a new ensemble in the cloud and join that from the premisses:

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/zTMoz_5bBDY?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/zTMoz_5bBDY?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>

As already mentioned above this approach is the easier way to get you going with a hybrid cloud solution.

### Images

Regardless of the way that you specify the image *(directly or indirectly)* the image needs to have some of the following characteristics:

* **Linux images**
* **RedHat or Debian packaging style**
* **No Java installation or Java 1.6+** If there is no java installed, fabric will do so for you, however it will not work if an incompatible java version is already installed.

You can also create an image of your own and use that instead. However, that may require different configuration preparation.
For Amazon EC2 you will need to specify the owner id of the private image, when registering the provider:

        fabric:cloud-service-add --owner myownerid cloudservers-us myidentity mycredential

### Locations & Hardware

Most cloud providers will give you the option to create containers on different location or using different hardware profiles.
You may wonder which are the proper values to use for your provider. Even though fabric provides completion for *all* configuration options, you still may want to get a list of them.

To list all of the avaialble locations:

        jclouds:location-list

The output for Amazon EC2 will look like this:

        [id]                             [scope]   [description]
        aws-ec2                          PROVIDER  https://ec2.us-east-1.amazonaws.com
          sa-east-1                      REGION    sa-east-1
            sa-east-1a                   ZONE      sa-east-1a
            sa-east-1b                   ZONE      sa-east-1b
          us-west-1                      REGION    us-west-1
            us-west-1a                   ZONE      us-west-1a
            us-west-1c                   ZONE      us-west-1c
            us-west-1b                   ZONE      us-west-1b
          us-east-1                      REGION    us-east-1
            us-east-1d                   ZONE      us-east-1d
            us-east-1c                   ZONE      us-east-1c
            us-east-1e                   ZONE      us-east-1e
            us-east-1a                   ZONE      us-east-1a
            us-east-1b                   ZONE      us-east-1b
          ap-northeast-1                 REGION    ap-northeast-1
            ap-northeast-1a              ZONE      ap-northeast-1a
            ap-northeast-1b              ZONE      ap-northeast-1b
          ap-southeast-1                 REGION    ap-southeast-1
            ap-southeast-1a              ZONE      ap-southeast-1a
            ap-southeast-1b              ZONE      ap-southeast-1b
          eu-west-1                      REGION    eu-west-1
            eu-west-1c                   ZONE      eu-west-1c
            eu-west-1a                   ZONE      eu-west-1a
            eu-west-1b                   ZONE      eu-west-1b
          us-west-2                      REGION    us-west-2
            us-west-2b                   ZONE      us-west-2b
            us-west-2c                   ZONE      us-west-2c
            us-west-2a                   ZONE      us-west-2a

To list all the available hardware profiles:

            jclouds:hardware-list

The hardware profiles for Amazon EC2 will look like:

            [id]                 [cpu] [cores]  [ram]
            cc1.4xlarge           32.0     8.0 23552.0
            cg1.4xlarge           32.0     8.0 22528.0
            cc2.8xlarge           88.0    16.0 61952.0
            t1.micro               1.0     1.0  630.0
            c1.medium              5.0     2.0 1740.0
            c1.xlarge             20.0     8.0 7168.0
            m1.large               4.0     2.0 7680.0
            m1.small               1.0     1.0 1740.0
            m1.medium              2.0     1.0 3750.0
            m1.xlarge              8.0     4.0 15360.0
            m2.xlarge              6.5     2.0 17510.0
            m2.2xlarge            13.0     4.0 35020.0
            m2.4xlarge            26.0     8.0 70041.0

You can do the same for images.

To make use of those information for creating a Fabric in the cloud you can specify them as options:

         fabric:container-create-cloud --provider aws-ec2 --location eu-west-1 --hardware m2.4xlarge mycontainer

The example above will create a new fabric container on the *eu-west-1* region and will use the *m2.4xlarge* hardware profile.

Here is a small clip that demonstrates how you can acquire those information from your cloud provider:

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/wVsazzjIlAo?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/wVsazzjIlAo?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>

### Fabric8 Examples in the Cloud
Fabric8 ships out of the box some really interesting example of how you can use Fabric8 with Camel and ActiveMQ. In the following clip you'll see how you can run on of these examples in the cloud using Fabric8's cloud container provider.
More specifically you'll see how:

* **Setup MQ containers in the cloud as master/slave**
* **Discover brokers using the Fabric runtime registry**
* **Run the camel example in the cloud that leverages broker discovery of Fabric**
* **Use the shell to retrieve live information about your running routes**

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/QBCxr5dHEHY?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/QBCxr5dHEHY?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>


