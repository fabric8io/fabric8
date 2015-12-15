## Fabric Annotations

<a href="http://kubernetes.io/">Kubernetes</a> supports annotations on any kubernetes resource such as on a [replication controller](replicationController.html) or [service](services.html) which are a great way to add arbitrary metadata. Note that annotation are limited in total to about 64Kb so any large metadata should be added via a URL rather than in place.

### Continuous Delivery Annotations

These annotations are to help support [Continuous Delivery](cdelivery.html) and to link deployments to the CI build which generated the image along with the source code changes and so forth.

| Annotation | Description | Example |
|------------|-------------|---------|
| fabric8.io/build-id| The build number used to generate this docker image and kubernetes resource | 2 |
| fabric8.io/build-url| The URL to view the build which generated the docker image and kubernetes resource | http://jenkins.vagrant.f8/job/gogsadmin-james3/2 |
| fabric8.io/git-branch| The git branch that generated the image | gogsadmin-james3-1.0.2 |
| fabric8.io/git-commit| The git commit ID of the source code used to generate the image | 5e1116f63df0bac2a80bdae2ebdc563577bbdf3c |
| fabric8.io/git-url| The URL to view the git commit history | http://gogs.vagrant.f8/gogsadmin/james3/commit/5e1116f63df0bac2a80bdae2ebdc563577bbdf3c |

### Secret Annotations

For more background on these annotation see the [Secret Annotations document](secretAnnotations.html); they are used to annotate OpenShift templates and [replication controllers](replicationController.html) with their secret requirements so that its easier to install applications using [gofabric8](https://github.com/fabric8io/gofabric8) and the ```gofabric8 secrets``` command:

| Annotation | Description | Example |
|------------|-------------|---------|
| fabric8.io/secret-ssh-key | Defines one or more named ssh keys (public and private key pairs) | ```mysecretname``` or ```secret1,secret2,anothersecret``` |
| fabric8.io/secret-ssh-public-key | Refers to a single or a bag of public keys | ```mysecretname.pub``` or ```mybagofsecrets[cheese.pub,beer.pub]``` |
| fabric8.io/secret-gpg-key | Refers to a GPG key  | mysecretname |

### Management Annotations

When using [Prometheus](http://prometheus.io) to [collect metrics](metrics.html) for monitoring your containers you may wish to add these annotations to the [service](services.html):

| Annotation | Description | Example |
|------------|-------------|---------|
| prometheus.io/scrape | Enable/disable the export of metrics to Prometheus | true |
| prometheus.io/path | the request path to find metrics to export to Prometheus | /metrics |
| prometheus.io/port | the request port to find metrics to export to Prometheus | 8080 |

