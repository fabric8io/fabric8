## Continuous Delivery

**Fabric8 Continuous Delivery** provides a [Continuous Delivery](http://en.wikipedia.org/wiki/Continuous_delivery) infrastructure built as a set of Kubernetes resources which is easy to [Install](fabric8OnOpenShift.html).

![Continous Delivery Diagram](http://upload.wikimedia.org/wikipedia/commons/7/74/Continuous_Delivery_process_diagram.png)

### Overview

**Fabric8 Continuous Delivery** consists of the following parts:

* [Jenkins](https://jenkins-ci.org/) for Building, Continuous Integration and creating Continuous Delivery pipelines.
* [Nexus](http://www.sonatype.org/nexus/) as the artifact repository for caching public artifacts and hosting canary and real release artifacts
* [Gogs](http://gogs.io/) for on premise git repository hosting and [GitHub](https://github.com/) for public hosting
* [SonarQube](http://www.sonarqube.org/) provides a platform to maintain code quality
* [Chat](chat.html) to support social integration between your teams and the infrastructure

You can choose to opt in or out of any of the micro services within Fabric8 Continuous Delivery; for example use any git repository hosting or Nexus installation. The [Chat](chat.html) integration works with [hubot](https://hubot.github.com/) so that it can work with any back end chat service such as IRC, Slack, HipChat, Campfire etc.
 
By default we try and integrate all the components closely out of the box so **Fabric8 Continuous Delivery** is an easy, 1 click install. e.g. by default Jenkins builds will use the local Nexus server for all downloads of maven artifacts and for all canary and full releases.

Since **Fabric8 Continuous Delivery** is built on top of Kubernetes we get an easy way to scale (e.g. Jenkins build slaves) together with letting us reuse [Kubernetes services](services.html) for service discovery to wire up the various components (e.g. for Jenkins to discover Nexus). If you opt out of any of the default implementations; you just need to create a [Kubernetes Service pointing to your external installation of the service](http://docs.openshift.org/latest/dev_guide/integrating_external_services.html).
 
 
### Automate Jenkins Jobs
 
Teams often have many git repositories with many artifacts and docker container builds. Manually maintaining individual [Jenkins](https://jenkins-ci.org/) build configurations for each git repository can be time consuming and error prone.

So we recommend the use of the [Jenkins Job DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki) to generate the Jenkins build jobs for your projects.

The Jenkins app comes with a template parameter **SEED_GIT_URL** which is the location of the git repository to clone in Jenkins for the Jenkins Job DSL used to generate the Jenkins builds. Any change in that git repository results in the `seed` job being rerun which then regenerates any of its Jenkins jobs. 

The `SEED_GIT_URL` parameter defaults to the value `https://github.com/fabric8io/default-jenkins-dsl.git` for the [default-jenkins-dsl](https://github.com/fabric8io/default-jenkins-dsl) project which provides an example set of scripts to iterate over your projects and create the necessary jobs for them. We hope soon those scripts will automatically setup CI / CD jobs for projects in the hosted [Gogs](http://gogs.io/) repositories (for now it will iterate over a github organisation and generates builds for matching projects).

### Installation
    
If you have [Apache Maven](http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) installed then the following instructions should get you going.
   
* type `osc login` to ensure you are logged in and [your machine is setup](setupLocalHost.html)
* set the `KUBERNETES_DOMAIN` environment variable which if you are running the [Fabric8 vagrant image](openShiftWithFabric8Vagrant.html) will be:

```
    export KUBERNETES_DOMAIN=vagrant.local
```
    
* type the following commands

```
    git clone https://github.com/fabric8io/fabric8-installer.git
    cd fabric8-installer
    cd cdelivery-core
    mvn install
    
    cd ../app 
    mvn install -DartifactId=fabric8-forge
```    

Currently there is [not a user created by default in gogs](https://github.com/fabric8io/fabric8/issues/4059) so you need to open the gogs web application and sign up. 

If you are running the [Fabric8 vagrant image](openShiftWithFabric8Vagrant.html) and have [setup your local machine's /etc/hosts file](setupLocalHost.html#adding-entries-in-etc-hosts) then you should be able to open [http://gogs.vagrant.local/](http://gogs.vagrant.local/) then click the `sign up` link.

For now use user `ceposta` and password `RedHat$1` as the user and password in gogs.

* Now in the [fabric8 console](console.html) if you click the **Projects** tab the **Repositories** sub tab should be available. This prompts you to login to gogs with your user and password (until we can get [single sign on working with gogs and OpenShift](https://github.com/gogits/gogs/issues/1271))

* Once you are logged in the **Repositories** tab should show a **Create Project** button on the top right. Click that and try create a project. On the second page of the wizard you get to choose which archetype to use as the start of the project. (If this combo box doesn't populate first time, its a little bug, go back in your browser and try again ;).

* A good start project is the **camel-cdi** archetype but try any of the archetypes you like the look of. 

* Once you click the **Execute** button the new git repository should be created, the initial code for the project generated from the archetype using the group and artifact IDs and Java package names you picked.

* You should now be able to see the new repository in the **Repositories** page and browse the repository in gogs via the **Browse** button and open the editor using the **Edit** button.
 
* The create project wizard should also now have triggered the Jenkins Job DSL to generate the CI / CD builds for this new project by triggering the **seed** build in Jenkins. If you look at the jenkins install - by default at [http://jenkins.vagrant.local](http://jenkins.vagrant.local) you should see either the **seed** build running or the new builds created for your project! 

* Now you might want to setup [Chat](chat.html) so that you see chat room notifications of builds and kubernetes resources change.
 
For more information see [Install Fabric8 on OpenShift Guide](fabric8OnOpenShift.html).
