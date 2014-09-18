### Configuration Questions

#### How can I edit fabric8's configuration via git?

Please see the [these instructions on working with git and fabric8](http://fabric8.io/gitbook/git.html)

#### How do I add new containers to an ensemble?

Check out [this article on adding a new container to an ensemble](http://fabric8.io/gitbook/registry.html#adding-containers-to-the-ensemble)

#### How do I configure fabric8 to use my local maven repository or a custom remote repository?

If you are running a fabric right now then clicking on the [default profile's io.fabric8.agent.properties](http://localhost:8181/hawtio/index.html#/wiki/branch/1.0/view/fabric/profiles/default.profile/io.fabric8.agent.properties) should let you view the current maven repositories configuration. Edit that file to add whatever maven repositories you wish.

The other option is to clone your git repository and edit this file then git push it back again. Please see the [these instructions for how to work with git and fabric8](http://fabric8.io/gitbook/git.html)

If you haven't yet created a fabric, in the fabric8 distribution you can edit the file **fabric/import/fabric/profiles/default.profile/io.fabric8.agent.properties** then if you create a new fabric it should have this configuration included.

#### Where did the child container provider go?

if you enable the [docker](http://fabric8.io/gitbook/docker.html) profile or run fabric8 via the [docker image](https://registry.hub.docker.com/u/fabric8/fabric8/) then fabric8 disables the **child** container provider by deafult. This is because if docker is available it is better that you use that to manage starting and stopping containers than the child container provider (which is suited for environments which don't support docker yet).

Similarly if you enable [kubernetes](http://fabric8.io/gitbook/kubernetes.html) or [OpenShift](http://fabric8.io/gitbook/openshift.html) container providers (or provision fabric8 within kubernetes/openshift) then fabric8 automatically disables **docker** and **child** container providers because if you are using kubernetes/OpenShift then its better we use that to start/stop containers than relying on local processes on a single machine.