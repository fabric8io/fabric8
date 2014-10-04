# Fabric JClouds Container Provider

## Description

The Jclouds Container Provider allows you to create a Fabric Container on the Cloud via Jclouds.

### Account & Image creation on the target cloud provider.

You will need to have an account with one of the supported providers and a java ready image. Java ready means with java installed and available on the basic path (/usr/bin, /usr/local/bin etc).

### Setup

From inside your Karaf/Fabric installation install the jcloud module for your provider (e.g. jclouds-aws-ec2 for Amazon EC2):

karaf@root>features:install jclouds-aws-ec2

Install the jclouds services module which provides service factories for compute services and blobstore services.

karaf@root>features:install jclouds-services

Create a compute service for your cloud provider:


karaf@root>config:edit org.jclouds.compute-ec2

karaf@root> config:propset provider aws-ec2

karaf@root> config:propset identiy CLOUD_PROVIDER_ID  (e.g. Access Key ID for Amazon)

karaf@root> config:propset credential CLOUD_PROVIDER_CREDENTIAL (e.g. Secret Access Key for Amazon)

karaf@root> config:update


Note: You may want to add provider specific configuration:


karaf@root> config:propset jclouds.ec2.ami-owners = EC2_OWNER_ID


Tip for EC2: The above will be used for quering images and will result in queries being faster.


You are now ready to install the jclouds container provider.


karaf@root>features:install fabric-jclouds


## Using the jclouds container provider

To create a new node using the jclouds container provider:

fabric:container-create --url jclouds://{PROVIDER_NAME}?imageId={IMAGE_ID}&locationId={LOCATION_ID}&group={GROUP}&user={USER} --profile {FABRIC_PROFILE} {AGENT_ID}

###Required parameters:

PROVIDER_NAME: The name of the provider as used in the compute service configuration (e.g. aws-ec2).

IMAGE_ID: The name of the provider as used in the compute service configuration (e.g. aws-ec2).

GROUP: The cloud provider group that the new node will join.

###Optional parameters:

LOCATION_ID: The location where the node should be created.

USER: The username on the target node, where the container will be installed (default: root).
