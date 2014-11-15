## Forge Addons

fabric8 comes with various [JBoss Forge](http://forge.jboss.org/) add ons to help improve your developer experience.

First [install Forge](http://forge.jboss.org/download) and run it:

    forge

you can install the Forge add ons via:

    addon-install --coordinate io.fabric8.forge:camel,2.0.2
    addon-install --coordinate io.fabric8.forge:kubernetes,2.0.2

You should be able to see the new commands that are installed via:

    kube<TAB>
    camel<TAB>

NOTE that you have to be a little patient; first time you try tab complete it can take a few seconds to figure out whats available :).


### Kubernetes

Before you run forge make sure your **KUBERNETES_MASTER** environment variable points to where OpenShift V3 is running.

If you want to run Forge against a local [Jube](http://fabric8.io/jube/getStarted.html) server then try this:

    export KUBERNETES_MASTER=http://localhost:8585/

You can now list pods / replication controllers / services via

    kubernetes-pod-list
    kubernetes-replication-controller-list
    kubernetes-service-list


### Applying JSON

If you are in a build which has [generated a kubernetes JSON file](mavenPlugin.html#generating-the-json) **target/classes/kubernetes.json** you can apply this via...

    kubernetes-apply --file target/classes/kubernetes.json

