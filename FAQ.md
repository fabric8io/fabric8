
### General Questions

General questions on all things fabric8.

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What is it?
fabric8 is an integration platform based on Apache ActiveMQ, Apache Camel, Apache CXF, Apache Karaf, Hawtio and others.

It provides automated configuration and deployment management to help make deployments easy, reproducible, and less human-error prone.

Take a look [at this blog post](http://www.christianposta.com/blog/?p=376) to see a more detailed treatment.

#### What does fabric8 do?

fabric8 (pronounced _fabricate_) lets you create and manage fabrics (or clusters) of applications, integrations and middleware.

Try reading the [overview](http://fabric8.io/gitbook/overview.html) to see if that helps give you an idea what fabric8 is.

#### Deprecations

FAB (Fuse Application Bundles) has been deprecated for the 1.1 release, and is scheduled to be removed in the following release. 

### Configuration questions

#### How can I edit fabric8's configuration via git?

Please see the [these instructions on working with git and fabric8](http://fabric8.io/gitbook/git.html)

#### How do I add new containers to an ensemble?

Check out [this article on adding a new container to an ensemble](http://fabric8.io/gitbook/registry.html#adding-containers-to-the-ensemble)

#### How do I configure fabric8 to use my local maven repository or a custom remote repository?

If you are running a fabric right now then clicking on the [default profile's io.fabric8.agent.properties](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/default.profile/io.fabric8.agent.properties) should let you view the current maven repositories configuration. Edit that file to add whatever maven repositories you wish.

The other option is to clone your git repository and edit this file then git push it back again. Please see the [these instructions for how to work with git and fabric8](http://fabric8.io/gitbook/git.html)

If you haven't yet created a fabric, in the fabric8 distribution you can edit the file **fabric/import/fabric/profiles/default.profile/io.fabric8.agent.properties** then if you create a new fabric it should have this configuration included.

### General Questions about using fabric8

#### How should I deploy fabric8 in a 2 data centre scenario where either data centre can fail?

A typical fabric8 installation has a ZooKeeper ensemble (a number of ZooKeeper servers connected together; either 1, 3, 5, 7 of them; usually 3 or 5) so that if there are failures or a network split, every node knows whether or not its part of the master split - to avoid the split brain problem. This lets fabric8 reliably create master
/ slaves and federated clusters such that there's no split brain / network partition issues.

If there are only exactly 2 data centres and either is allowed to fail and the other takes over; then there's no real way to solve this the single-ZooKeeper ensemble route automatically; since there's no way to achieve quorum in either data centre. (Its trivial to have, say, 2 ZK servers in one DC X and 1 in the DC Y; but then if DC X fails; you can't achieve quorum automatically.

There’s a few ways this could work; it depends on trade-offs really. Our recommended approach is:

* run 2 ZK ensembles in each DC. This then means things work fine; either DC can fail at any time. It does mean that you cannot have one global master service any more; you can only have DC-local masters (e.g. a master broker in each DC). Then you store/forward between the DCs. There's a [pending issue](https://github.com/fabric8io/fabric8/issues/622) to try make a 2-data centre and 2-fabric deployment appear more like a single logical fabric (by having a form of store/foward bridging at the ZK level).

If you want to favour one DC so that its the master and have some manual recovery mechanism you could consider this approach (though its not really recommended):

* pick one DC as the master and run more ZK servers there. If that DC fails, you have to manually decide its down (and make sure the ZK servers are manually taken down) then run some more in the remaining DC. i.e. a manual failover process if the master fails (though automatic if the slave fails).

#### I cannot start fabric8 on Windows

There is a known issue with Java and Windows when using IP6 capable network. You may see errors such as ``java.net.SocketException: Permission denied: no further information``. To resolve this set the ``KARAF_OPTS`` to the following in the ``bin/setenv.bat`` file.

    KARAF_OPTS="-Djava.net.preferIPv4Stack=true"

For more details see the [IP6 Java network guide](http://docs.oracle.com/javase/7/docs/technotes/guides/net/ipv6_guide/) and this [knowledgebase solution](https://access.redhat.com/site/solutions/757533).

### Known issues about using fabric8

#### Why does the welcome screen print two times?

When starting fabric8 using `bin\fabric8` the welcome screen may be printed two times. This will be resolved in a future release of fabric8, by upgrading to Apache Karaf 2.4.0 when it becomes available.
