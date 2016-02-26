## Forge Addons

fabric8 comes with various [JBoss Forge](http://forge.jboss.org/) add ons to help improve your developer experience.

First [install Forge](http://forge.jboss.org/download) and run it:

    forge

you can install the Forge add ons via:

    addon-install --coordinate io.fabric8.forge:camel,2.2.129
    addon-install --coordinate io.fabric8.forge:camel-commands,2.2.129
    addon-install --coordinate io.fabric8.forge:devops,2.2.129
    addon-install --coordinate io.fabric8.forge:kubernetes,2.2.129

Notice the version number (current 2.2.129) is the fabric8-forge release. You can find the [latest release number](https://github.com/fabric8io/fabric8-forge/releases) on github. 

You should be able to see the new commands that are installed via:

    camel<TAB>
    devops<TAB>
    kubernetes<TAB>

You have to be a little patient; first time you try tab complete it can take a few seconds to figure out what's available.


### Camel

The Camel Forge addon enables developers to edit Maven based source code projects with Camel. For example to add or edit endpoints.

To setup a Maven project for Apache Camel then use the command:

    camel-setup

To add a new Camel endpoint using a wizard to select the options then type:

    camel-add-endpoint

### Fabric8 Camel Maven Plugin

The Camel forge addon provides a command to validate the Camel endpoints in the source code (both Java and XML). We provide a [Camel Maven Plugin](camelMavenPlugin.md) that is able to run this command from Maven command line, and report invalidate Camel endpoints. You can enable this plugin in your Maven projects as part of the build process, to catch invalid uris before you run the application.

### Camel Commands

The Camel Commands Forge addon enables developers to manage/interact with running Camel applications, using the Apache Camel commands.

The running Camel application must have Jolokia included which is what is being used by the addon to communicate with the running Camel applications.

To connect to a running Camel application type:

    camel-connect

After connection, then the management commands should be available such as to list all the Camel Context's in the JVM type:

    camel-context-list
    

### DevOps

The **DevOps** commands help you to 

* create new projects
* setup existing project for docker/fabric8 using the **fabric8-setup** command
* configure Kubernetes Service
* configure the DevOps configuration of a project via the [fabric8.yml file](fabric8YmlFile.html)
* generate new integration or system tests for a project


To setup a Maven project for fabric8 and Docker then use the command:

    fabric8-setup

If you are inside a project then use the command:

    devops-edit
    
to open the edit devops command which is a wizard that lets you configure the Docker and Fabric8 build metadata along with setting up the Jenkins workflow CD pipeline and linking the project to a team chat room and issue tracker.
    
### Kubernetes

Before you run forge make sure your **KUBERNETES_MASTER** environment variable points to where OpenShift V3 is running.

The `oc` or `kubectl` client are more powerful than the kubernetes forge addon, and its recommended to use these clients.

#### Applying JSON

If you are in a build which has [generated a kubernetes JSON file](mavenPlugin.html#generating-the-json) **target/classes/kubernetes.json** you can apply this via...

    kubernetes-apply --file target/classes/kubernetes.json

