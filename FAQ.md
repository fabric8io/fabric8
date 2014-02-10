### General Questions

General questions on all things fabric8.

#### What is the license?

fabric8 uses the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt).

#### What does fabric8 do?

fabric8 (pronounced _fabricate_) lets you create and manage fabrics (or clusters) of applications, integrations and middleware.

### Problems/General Questions about using fabric8

#### How should I deploy fabric8 in a 2 data centre scenario where either data centre can fail?

A typical fabric8 installation has a ZooKeeper ensemble (a number of ZooKeeper servers connected together; either 1, 3, 5, 7 of them; usually 3 or 5) so that if there are failures or a network split, every node knows whether or not its part of the master split - to avoid the split brain problem. This lets fabric8 reliably create master
/ slaves and federated clusters such that there's no split brain / network partition issues.

If there are only exactly 2 data centres and either is allowed to fail and the other takes over; then there's no real way to solve this the single-ZooKeeper ensemble route automatically; since there's no way to achieve quorum in either data centre. (Its trivial to have, say, 2 ZK servers in one DC X and 1 in the DC Y; but then if DC X fails; you can't achieve quorum automatically.

There’s a few ways this could work; it depends on trade-offs really. Our recommended approach is:

* run 2 ZK ensembles in each DC. This then means things work fine; either DC can fail at any time. It does mean that you cannot have one global master service any more; you can only have DC-local masters (e.g. a master broker in each DC). Then you store/forward between the DCs. There's a [pending issue](https://github.com/fabric8io/fabric8/issues/622) to try make a 2-data centre and 2-fabric deployment appear more like a single logical fabric (by having a form of store/foward bridging at the ZK level).

If you want to favour one DC so that its the master and have some manual recovery mechanism you could consider this approach (though its not really recommended):

* pick one DC as the master and run more ZK servers there. If that DC fails, you have to manually decide its down (and make sure the ZK servers are manually taken down) then run some more in the remaining DC. i.e. a manual failover process if the master fails (though automatic if the slave fails).


