## Secret Annotations

Pods (and PodSpecs) can reference secrets via volumes in kubernetes and if the secrets are not yet created, Kubernetes will wait to instantiate the pod.

Secret Annotations provide a way to annotate a PodTemplate with the kind of secret that is required when mounting secrets into volumes so that tools such as the [gofabric8](https://github.com/fabric8io/gofabric8) command line installer can automatically generate ssh or gpg keys for you or let you import them at install time.

e.g. so that run a secret install step after you create Templates or ReplicationControllers to install/create any required secrets automatically.

This means that Kubernetes `List` or OpenShift `Template` resources can refer to secrets and be installed; but then pods which need secrets won't activate until a user or tool creates the associated secrets. It also means any mechanism can be used to install/create the secrets. Hopefully over time more tools cna support these annotation too.

Longer term this metadata should really be encoded concretely in the SecretVolume directly in the PodTemplate.

### Annotations

We use different annotations for different kinds of secrets

### SSH keys

Use an annotation with the key `fabric8.io/secret-ssh-key`

    fabric8.io/secret-ssh-key = mysecretname

This will indicate that the secret called `mysecretname` needs to be created as an SSH public and private key pair

If you need multiple ssh key secrets then use a comma separated list

    fabric8.io/secret-ssh-key = secret1,secret2,anothersecret

This will generate/import 3 secrets which all contain public/private keys.

#### SSH public keys

Use an annotation with the key `fabric8.io/secret-ssh-public-key`

Often you need to create secrets that only contain the public key; so that the private key is not visible in a pod.

To do this name your secret with `.pub` on the end.

    fabric8.io/secret-ssh-public-key = mysecretname.pub

This will indicate that the secret called `mysecretname.pub` needs to be created as secret which only contains the SSH public key from the secret `mysecretname` which has the private and public key.

i.e. there will be 2 secrets created

* mysecretname contains a public and private ssh key
* mysecretname.pub contains _just_ the public ssh key


##### folders of public keys

Its common to want to create a single secret that contains a number of public keys inside the same secret that is then mounted to single volume.

To do this name your secret appending `[secret1.pub,secret2,secret3]` on the end of the secret name.

    fabric8.io/secret-ssh-public-key = mybagofsecrets[cheese.pub,beer.pub]

This will create a secret called `mybagofsecrets` which contains files `cheese.pub` and `beer.pub` for the public keys of the SSH key secrets `cheese` and `beer`

i.e. there will be 3 secrets created

* cheese contains a public and private ssh key
* beer contains a public and private ssh key
* mybagofsecrets contains the public keys `cheese.pub` and `beer.pub`


#### GPG

Use an annotation of the form

    fabric8.io/secret-gpg-key = mysecretname

#### Maven settings

Use an annotation of the form

    fabric8.io/secret-maven-settings = jenkins-maven-settings

This will import a `settings.xml` from folder jenkins-maven-settings, if the folder is not found it will use the default maven settings.xml [here](https://github.com/fabric8io/gofabric8/blob/master/default-secrets/mvnsettings.xml)

#### Docker auth config

Use an annotation of the form

    fabric8.io/secret-docker-cfg = jenkins-docker-cfg

This will import a `config.json` from folder jenkins-docker-cfg, if none is found an empty secret is generated.

#### GitHub API token

Use an annotation of the form

    fabric8.io/secret-github-api-token = jenkins-github-api-token

This will import a file name of `apitoken` from folder jenkins-github-api-token, if none is found an empty secret is generated.

### Mounting SSH keys

Mounting all secretes end up being a volume with a file for each data entry inside the secret.

| Annotation | Files in the secret volume folder |
|------------|-----------------------------------|
| fabric8.io/secret-ssh-key | id_rsa.pub id_rsa  |    
| fabric8.io/secret-ssh-public-key | id_rsa.pub  |    

### Example

This is an example folder structure that the fabric8 release uses itslef when creating its CD environment.  

Running `gofabric8 secrets` from the root folder..

    (root)
     +- jenkins-git-ssh                    
     |   +- ssh-key
     |   +- ssh-key.pub

     +- jenkins-release-gpg
     |   +- pubring.gpg
     |   +- secring.gpg
     |   +- trustdb.gpg

     +- jenkins-docker-cfg
     |   +- config.json

     +- jenkins-github-api-token
     |   +- apitoken

     +- jenkins-maven-settings
     |   +- settings.xml

Once secrets have been added in the example of Jenkins we can mount those secrets using [kubernetes-workflow](https://github.com/fabric8io/kubernetes-workflow#using-secrets) into pods that run our workflows..


    node('kubernetes'){
        echo 'worked'
        kubernetes.pod('buildpod')
          .withImage('fabric8/builder-openshift-client')
          .withSecret('jenkins-docker-cfg','/home/jenkins/.docker')
          .withSecret('jenkins-maven-settings','/home/jenkins/.m2')
          .inside {

            checkout scm
            sh "cat `/home/jenkins/.docker/config.json`"
            sh 'mvn clean install deploy'

        }
    }
