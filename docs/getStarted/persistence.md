## Persistence

Various apps in the fabric8 microservice platform require persistence (e.g.gogs, nexus, jenkins). The apps use the kubernetes resource `PersistentVolumeClaim` to specify their needs for a `PersistentVolume`

So when you deploy fabric8 via [gofabric8](https://github.com/fabric8io/gofabric8) by default this will create a number (5-6) of `PersistentVolumeClaim` resources.

If you are using [minikube](https://github.com/jimmidyson/minikube) or [minishift](https://github.com/jimmidyson/minishift) then `gofabric8` automatically spins up the equivalent `PersistentVolume` resources for you using `HostPath` based PersistentVolumes - so things should just work!

However on any other kind of kubernetes or openshift cluster; the PVCs will be matched to any suitable PV resources that are already available. Otherwise you will need to create 5 or 6 PV instances using whatever PV implementation you wish to use (e.g. EBS on EC2 or the equivalent on GCE / Azuer, or NFS/Gluster/Ceph when on premise etc).

We hope to take advantage of kubernetes 1.4 dynamic `PersistentVolume` provisioning to automatically spin these up to simplify installation. 

### Viewing the PV and PVCs

To see the current `PersistentVolumeClaim` and `PersistentVolume` resources use the following commands

    kubectl get pvc
    kubectl get pv

When things are working correctly you should see all the `PersistentVolumeClaim` resources bound to some `PersistentVolume` resource. When thats true then all the various persistent apps in fabric8 will startup.

### Disabling persistence

If you want to try out fabric8 without setting up all the `PersistentVolume` instances for your cluster you can just disable persistence

To disable persistence just add the `--no-pvc` command argument when using `gofabric8 deploy`

    gofabric8 deploy -y --no-pvc

Then all `PersistentVolumeClaim` volumes will be converted to `EmptyDir` volumes instead to avoid needing any `PersistentVolume` resources to be created.

### Troubleshooting

If you hit any issues with this please [create up an issue](https://github.com/fabric8io/gofabric8/issues) immediately and we'll do our best to get you going.

Or please [join us in the community on IRC/slack/stackoverflow](http://fabric8.io/community/)!
