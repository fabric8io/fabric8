# MQ Fabric support

This module providers support for using Fabric to manage groups of brokers efficiently and to create tools to help visualise and manage things.

Firstly brokers are put into logical groups. A logical group is then used for discovery. So messaging clients just connect to a group & they don't care which broker in the group they connect to.

There are a few different ways to configure Logical Brokers which map to 1 or more physical brokers.

## 1. Master / Slave Broker

In master slave we create a logical broker in a group. e.g. group A, broker1 and broker2.

Now we've 2 logical brokers. Each of these 2 logical brokers gets a Fabric profile. We can run 1 or more instances of each.

If we run 2 instances of broker1 profile in 2 separate containers; one is the master the other is the slave (with failover).

## 2. Replicated Broker

In replicated mode you run N replicas of the same logical broker. Typically you'd run, say, 3 replicas; typically inheriting from the **mq-replicated** profile.

So you'd have 1 profile for a replica set of brokers and you deploy 3 instances of that container.

## 3. N + 1 Broker

In N + 1 you define N brokers (broker name and configurations) in a group. e.g. group A has broker1 and broker2. Then you create N+1 containers each having all the N brokers inside.

This maps to a single profile for the N+1 group, which contains broker1 and broker2; then you'd run 3 of these containers; with 2 of the containers being master and one being slave to the other 2 brokers.

The **standby.group** (which defaults to the group) is used to ensure that each container is only master of 1 logical broker; to avoid running 3 containers and 1 of them being master of both broker1 and broker2

## Implementation details

Each logical Master/Slave broker, Replicated broker set, or N + 1 group of brokers maps to a Profile in Fabric. Each will have a single broker inside the profile configuration - apart from N+1 Broker which will have N broker configurations.

Broker configurations are defined by the file: **org.fusesource.mq.fabric.server-$brokerName.properties** inside the profile

