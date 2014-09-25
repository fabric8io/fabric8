### Questions On Using Fabric8

#### How can I automate the creation of a fabric?

Many customers want an easy, repeatable way to spin up a fabric and all the various containers they need in an automated way such as for [Continuous Deployment](http://fabric8.io/gitbook/continuousDeployment.html).

To do this fabric8 has an [Auto Scaler](http://fabric8.io/gitbook/requirements.html) which allows you to define how many instances of each profile you need and the auto scaler will automatically create the containers you need; using the available resources and automatically create new containers if there is a hardware or software failure.

#### How should I compose profiles which contain blueprint or spring XML files?

[Profiles](http://fabric8.io/gitbook/profiles.html) are designed so that they can be _combined_ together into a container; or they can use inheritance to _override_ properties.

If you wish to combine profiles together you need to make sure that if you want to _combine_ rather than _override_ you need to ensure that the properties files in each profile use either different file names or different keys.

For example if you have 3 different blueprint/spring XML files inside each profile which you want to combine so that all 3 will be executed; name each one differently and use a different key name in the **io.fabric8.agent.properties** files. e.g. the [bank1](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/example/camel/loanbroker/mq.bank1.profile), [bank2](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/example/camel/loanbroker/mq.bank2.profile) and [bank3](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/example/camel/loanbroker/mq.bank3.profile) profiles in the [MQ based loan broker example](https://github.com/fabric8io/fabric8/tree/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/example/camel/loanbroker/) all use a different blueprint XML file and key in the [io.fabric8.agent.properties file](https://github.com/fabric8io/fabric8/blob/master/fabric/fabric8-karaf/src/main/resources/distro/fabric/import/fabric/profiles/example/camel/loanbroker/mq.bank1.profile/io.fabric8.agent.properties#L22)

#### How should I deploy fabric8 in a 2 data centre scenario where either data centre can fail?

A typical fabric8 installation has a ZooKeeper ensemble (a number of ZooKeeper servers connected together; either 1, 3, 5, 7 of them; usually 3 or 5) so that if there are failures or a network split, every node knows whether or not its part of the master split - to avoid the split brain problem. This lets fabric8 reliably create master
/ slaves and federated clusters such that there's no split brain / network partition issues.

If there are only exactly 2 data centres and either is allowed to fail and the other takes over; then there's no real way to solve this the single-ZooKeeper ensemble route automatically; since there's no way to achieve quorum in either data centre. (Its trivial to have, say, 2 ZK servers in one DC X and 1 in the DC Y; but then if DC X fails; you can't achieve quorum automatically.

There’s a few ways this could work; it depends on trade-offs really. Our recommended approach is:

* run 2 ZK ensembles in each DC. This then means things work fine; either DC can fail at any time. It does mean that you cannot have one global master service any more; you can only have DC-local masters (e.g. a master broker in each DC). Then you store/forward between the DCs. There's a [pending issue](https://github.com/fabric8io/fabric8/issues/622) to try make a 2-data centre and 2-fabric deployment appear more like a single logical fabric (by having a form of store/foward bridging at the ZK level).

If you want to favour one DC so that its the master and have some manual recovery mechanism you could consider this approach (though its not really recommended):

* pick one DC as the master and run more ZK servers there. If that DC fails, you have to manually decide its down (and make sure the ZK servers are manually taken down) then run some more in the remaining DC. i.e. a manual failover process if the master fails (though automatic if the slave fails).

#### How should I use Spring 4 with fabric8?

Fabric8 has awesome support for Spring 4 with the [Spring Boot Container]((http://fabric8.io/gitbook/springBootContainer.html).

If you've been previously using OSGi or [Apache Karaf](http://karaf.apache.org/) as your application server then Spring 4 poses a challenge as its not currently available as valid OSGi bundles. So to use Spring 4 we recommend you consider moving to the [Java Container]((http://fabric8.io/gitbook/javaContainer.html) in particular the [Spring Boot Container]((http://fabric8.io/gitbook/springBootContainer.html)


