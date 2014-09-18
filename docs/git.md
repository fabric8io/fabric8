## Git

Fabric8 stores all of its runtime configuration in git repositories; so that all configuration changes are audited and versioned.

We can then use any git tooling to do a diff; revert changes or merge changes between branches or different repositories. e.g. merge changes from one environment to another using code review tools, Pull Requests or Gerrit etc.

### How does git work in fabric8?

Fabric8 uses the jgit library (so there does not need to be the 'git' binary installed on a machine).

Under the covers Fabric8 implements a replicated git cluster as follows:

* each container has its own git clone of the configuration
* a git master container is elected which becomes the master remote repository (its public git URL becomes the URL for the 'origin' repository in each container's git repo)
* all configuration changes are then pushed to the master
* when changes occur all containers pull from the master's repository
* if the git master container stops, another container is elected the leader

So each container has a full history of all the changes and each container also acts as a backup replica of the configuration. There's also no single point of failure. Pretty cool eh! :)

### Using git with fabric8

 You can make configuration changes to fabric8 rather like working with Heroku or OpenShift; cloning the git repository; editing the files using any editor then git commiting and pushing the changes back.

 The simplest way to clone the git repository is to:

 * view a container page in the web console
 * open the URL's tab (or [this link](http://localhost:8181/hawtio/index.html#/fabric/container/root?tab=URLs) should take you straight there for the root container)
 * then click the 'copy to clip board' button next to the first **Git** field
 * you should now be able to open a terminal and paste the command to perform a git clone

Or if you're more of a CLI kinda person; just type this in a shell

    git clone -b 1.0 http://admin:admin@localhost:8181/git/fabric
    cd fabric

This assumes the login/password of admin/admin; you probably want to change that password.

You should then have the version 1.0 branch checked out where you can edit the various profiles (in fabric/profiles folder) using any text editor. When you want to submit any changes back, commit them and then type

    git commit -a -m "my new changes"
    git push

And hey presto, fabric8 should update and any containers running the version/branch and profiles you modified should update.

### Working with the external git repositories

Instead of using clustered git repository provided by the Fabric8, you can store your configuration in the external 
git repository of your choice. You can specify external repository URL when creating new Fabric8 instance.

    fabric8:create --external-git-url=git@github.com:john/johnsproject.git
    
Keep in mind that although you introduce a single point of failure (single external Git repository), Fabric8 can still
operate even if the repository is down for the moment. All the changes performed by containers on their local
repositories (i.e. all changes to the configuration) will be pushed and distributed to the other containers as soon as 
the external Git repository becomes available again.

### Using a HTTP proxy with the git cluster

Using the built-in git cluster in fabric will communicate directly between the nodes over HTTP. If a HTTP proxy is required for communication, then you can configure the git proxy in fabric by configuring the ```GitProxyService```.

From the CLI type:

    fabric:profile-edit --pid io.fabric8.git.proxy/proxyHost=servername default
    fabric:profile-edit --pid io.fabric8.git.proxy/proxyPort=portNumber default

Notice you must specify both a hostname and port to use.

It is also possible to specify a nonProxyHost to allow some nodes to not use the HTTP proxy. Multiple hosts is separated using the ```|``` charachter. Remember to qoute the pid value using ```'``` quotes when you use the ```|``` charachter, as the ```|``` is used for chaining commands.

    fabric:profile-edit --pid 'io.fabric8.git.proxy/nonProxyHosts=someServer|somerOtherServer' default

Notice that by default nonProxyHosts will not proxy any URIs that is localhost as access to itself, does not require to use the HTTP proxy.
But if you configure this option, then remember to add ```localhost|127.*``` to still not proxy any localhost addresses.

You can also configure the ```GitProxySerivce``` from the web console, by selecting the fabric profile, and click the ```Configuration``` button.
