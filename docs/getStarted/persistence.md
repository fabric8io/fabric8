## Persistence

Various apps in the fabric8  platform require persistence (e.g.gogs, nexus, jenkins). The apps use the kubernetes resource `PersistentVolumeClaim` to specify their needs for a `PersistentVolume`

So when you deploy fabric8 via [gofabric8](gofabric8.html) by default this will create a number (5-6) of `PersistentVolumeClaim` resources.

### Viewing the PV and PVCs

To see the current `PersistentVolumeClaim` and `PersistentVolume` resources use the following commands:

    kubectl get pvc
    kubectl get pv

When things are working correctly you should see all the `PersistentVolumeClaim` resources are `Bound` to some `PersistentVolume` resource. When thats true then all the various persistent apps in fabric8 will startup.

### MiniKube and MiniShift

If you are using [minikube](https://github.com/jimmidyson/minikube) or [minishift](https://github.com/jimmidyson/minishift) then `gofabric8` automatically spins up the equivalent `PersistentVolume` resources for you using `HostPath` based PersistentVolumes - so things should just work!

Though if you create new apps on the fly that uses persistence after you have ran the [gofabric8 start](gofabric8.html) command then you may have `PersistentVolumeClaim` resources which are stuck `Pending`.

If so then you can dynamically create new `PersistentVolume` resources for any pending `PersistentVolumeClaim` resources via the command:

    gofabric8 volumes
    
Ultimately we will enable dynamic `PersistentVolume` creation in [minikube](https://github.com/jimmidyson/minikube) and [minishift](https://github.com/jimmidyson/minishift) to avoid this command being necessary.    


### Other clusters

We use Kubernetes dynamic `PersistentVolume` provisioning to automatically create PVs when a `PersistentVolumeClaim` needs one.

This is done by creating a [StorageClass](http://kubernetes.io/docs/user-guide/persistent-volumes/#storageclasses) which is configured to point to cloud persistence implementations like EBS, GlusterFS, Cinder etc

All fabric8 related `PersistentVolumeClaim` resources have a `volume.beta.kubernetes.io/storage-class=standard` annotation that is used to match which StorageClass to use.

Example:

    apiVersion: "v1"
    kind: "PersistentVolumeClaim"
    metadata:
    annotations:
        volume.beta.kubernetes.io/storage-class: "standard"
    name: "jenkins-jobs"
    spec:
    accessModes:
    - "ReadWriteOnce"
    resources:
        requests:
        storage: "100Mi"

So when setting up a cluster we just need to choose which PV implemenation to use and add it to your `StorageClass` e.g.

__GKE__

    cat <<EOF | kubectl create -f -
    kind: StorageClass
    apiVersion: storage.k8s.io/v1beta1
    metadata:
      name: standard
    provisioner: kubernetes.io/gce-pd
    parameters:
      type: pd-standard
    EOF

__AWS__

      cat <<EOF | kubectl create -f -
      kind: StorageClass
      apiVersion: storage.k8s.io/v1beta1
      metadata:
        name: standard
      provisioner: kubernetes.io/aws-ebs
      parameters:
        type: gp2
      EOF

For a full list of StorageClass examples take a look at the kubernetes docs http://kubernetes.io/docs/user-guide/persistent-volumes/#parameters

### Disabling persistence

If you want to try out fabric8 without setting up all the `PersistentVolume` instances for your cluster you can just disable persistence

To disable persistence just add the `--pv=false` command argument when using `gofabric8 deploy`

    gofabric8 deploy -y --pv=false

Then all `PersistentVolumeClaim` volumes will be converted to `EmptyDir` volumes instead to avoid needing any `PersistentVolume` resources to be created.

### Troubleshooting

If you hit any issues with this please [create up an issue](https://github.com/fabric8io/gofabric8/issues) immediately and we'll do our best to get you going.

Or please [join us in the community on IRC/slack/stackoverflow](http://fabric8.io/community/)!
