## Secret Annotations

Pods (and PodSpecs) can reference secrets via volumes in kubernetes and if the secrets are not yet created, Kubernetes will wait to instantiate the pod.

Secret Annotations provide a way to annotate a PodTemplate with the kind of secret that is required when mounting secrets into volumes so that tools such as the [gofabric8](https://github.com/fabric8io/gofabric8) command line installer can automatically generate ssh or gpg keys for you or let you import them at install time.

e.g. so that run a secret install step after you create Templates or ReplicationControllers to install/create any required secrets automatically.

### Annotations

We use different annotations for different kinds of secrets

### SSH keys

Use an annotation of the form

    fabric8.io/secret-ssh-key = mysecretname

This will indicate that the secret called `mysecretname` needs to be created as an SSH public and private key pair

If you need multiple ssh key secrets then use a comma separated list

    fabric8.io/secret-ssh-key = secret1,secret2,anothersecret

This will generate/import 3 secrets which all contain public/private keys.

#### SSH public keys

Often you need to create secrets that only contain the public key; so that the private key is not visible in a pod.

To do this name your secret with `.pub` on the end.

    fabric8.io/secret-ssh-key = mysecretname.pub

This will indicate that the secret called `mysecretname.pub` needs to be created as secret which only contains the SSH public key from the secret `mysecretname` which has the private and public key.

i.e. there will be 2 secrets created

* mysecretname contains a public and private ssh key
* mysecretname.pub contains _just_ the public ssh key


#### SSH public key folders

Its common to want to create a single secret that contains a number of public keys inside.

To do this name your secret appending `[secret1.pub,secret2,secret3]` on the end.

    fabric8.io/secret-ssh-key = mybagofsecrets[cheese.pub,beer.pub]
    
This will create a secret called `mybagofsecrets` which contains files `cheese.pub` and `beer.pub` for the public keys of the SSH key secrets `cheese` and `beer`
 
i.e. there will be 3 secrets created

* cheese contains a public and private ssh key
* beer contains a public and private ssh key
* mybagofsecrets contains the public keys `cheese.pub` and `beer.pub` 


#### GPG

Use an annotation of the form

    fabric8.io/secret-gpg-key = mysecretname
    
### Mounting SSH keys

Mounting all secretes end up being a volume with a file for each data entry inside the secret.

    