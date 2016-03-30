### Troubleshooting

With Linux, Docker, Kubernetes / OpenShift plus optionally Vagrant / VirtualBox / VMs there are various things that can go wrong unfortunately! We really try to make stuff Just Work (tm) but now and again things fall through the cracks.

This page tries to describe all the things you can do to try figure out why things are not working.

#### Check what the pods are doing:

You can view the current state of pods via:

    oc get pods

Or watch for when they change (e.g. start Running or become Ready or Terminate) via:

    oc get pods -w

If you have issues with the [console](../console.html) its worth checking that the `fabric8` and `router` pods are running and in a Ready state.

If things are not quite running then this can give more help

    oc describe pod fabric8-abcd

Where `fabric8-abcd` is the name of the pod you are diagnosing. 

If there's no pod but there is a [Replication Controller](../replicationControllers.html) then try this for an RC called `foo`:

    oc describe rc foo

    
#### Cannot access services from your browser

Sometimes services are working along with pods but you can't access them from your host. 
 
Its worth checking on OpenShift to see if there's a route for your service and what the host name is:

    oc get route
    
On OS X sometimes DNS gets a bit confused, so if things are running but you can't access them from your laptop try:

 	sudo dscacheutil -flushcache sudo killall -HUP mDNSResponder

I've sometimes seen landrush plugin for vagrant get confused too - wonder if this helps?

    vagrant landrush restart

If its DNS related you can cheat and add something like this to your /etc/hosts

    172.28.128.4 vagrant.f8 fabric8.vagrant.f8 gogs.vagrant.f8 jenkins.vagrant.f8 nexus.vagrant.f8
    
#### If all else fails try the logs

here's how to look inside the [openshift logs](vagrant.html#looking-at-the-openshift-logs)

    
    