# Git and Fabric

The basic idea is to use git to store all configuration changes to Fabric so that we get an audit log, versioning, history, delta & easy revert. Plus we can then branch between development, testing, UAT and production.

More detail in [issue FABRIC-346](http://fusesource.com/issues/browse/FABRIC-346).

## Approach

* each agent has a local clone of the git repo for configuration
* write all configuration changes to a local git repo first; we then push it remotely to the master git repo
* FabricAgent then discovers changes to the configuration and updates (as today)

## Master git repo options

Folks could use some external git repo somewhere - but this introduces complexity and a single point of failure.

So its much nicer to have a master git repo inside the fabric so its using the same fabric security as the maven repo, JMX, ssh, web apps etc.

We then elect a leader, register it in ZK so all containers know where the master is. We then need to edit the ${karaf.home}/data/fabricConfig/.git/config with the new URL if the master changes.

Also since every container has a copy of the git repo, we have a HA configuration repo.

## Change notification options

When git changes we need to notify the FabricAgent (as we do now with pure ZK)

* we could keep using ZK for configuration. if we do we could either:
    * update ZK after we update git. (To be sure we write to git, push, then pull, then re-read the files and write to ZK).
    * have a git master which has a push hook that writes to ZK; we'd then use ZK for failover of the git master

* or we could ditch ZK for configuration and just use git. We could then either
    * periodically pull and look for file changes
    * use ZK to notify of changes by the git master which then causes git pulls. However this could cause _git pull broastcast storms_. To work around this we could
        * add a random timer before pulling (which kinda defeats the point - may as well just do periodic polls using a random timer)
        * have a kind of tree; so we have a maximum of N nodes pulling from the master; then each of these N nodes has at most N nodes watching it for changes. So we have a tree of git repo chains so there's not too much of a storm

Reasons for ditching ZK for configuration are

* ZK is sensitive to size which limits what we can put in a profile's directory.
* ZK isn't awesome on dodgy WANs with occasional downtime. Git is great for this as we can pull from time to time in this cases

Either way folks can update git in any way - via Fabric tools directly or via IDEs or git tooling directly (e.g. merging branches with gerrit). So whether we keep config in git or not, we should have a git master push notification which then updates ZK. Containers can always not use ZK if they wish and stick with git for configuration

## Recommended Approach

* containers have a local git clone
* config changes write to git first then push to the master git repo
* have master git repo elected/configured in the fabric
* the master git repo has a push hook that then updates ZK (we can configure a filter to decide how much/little of git goes into ZK)
* failure to the master fails over via ZK to a new master

## Other ideas once a profile is a directory in git

If a profile becomes a versioned directory we could introduce the idea of 'overlays'. e.g. imagine if we defined a configuration file that allows us to overlay files from the profile's directory onto some container.

e.g. we could allow any distro of Tomcat, Jetty, Karaf, JBoss, whatever to be installed from a URL / maven coordinate. Then have files from the profile directory overlaid onto the container installation (e.g. updating the config files or adding deployment units into the deploy directory).

So the profile could just focus on starting/stopping a sub process & using the files in git as a versioned overlay mechanism for updating configuration in the container.

Then there's no need for deep fabric integration into all containers. Folks could easily install local containers from some tarball; overwrite on a per profile basis files as required & do deployments using files in git (or using URLS / maven coordinates).

e.g. imagine this profile directory tree in git. We're using real files here rather than URLs / maven coordinates for simplicity

```
myProfile
  overlay/
    etc/something.cfg
    deploy/
      myapp.jar
      anotherthing.jar
  overlay.json
```

where overlay.json could use the existing [container specific json files for starting and stopping stuff](https://github.com/fusesource/fuse/tree/master/process/process-manager/src/main/resources)

```
{
  "containerUrl": "mvn:org.apache.openejb/apache-tomee/1.5.0/tar.gz/plus",
  "controller": "tomcat"
}
```

See [how to use a JSON kind for controlling containers](http://fuse.fusesource.org/fabric/docs/process-manager.html#Managing_processes_like_Tomcat__Jetty__HQ_Agent)

Another approach would be to use [RHQ Ant tasks](http://fusesource.com/issues/browse/FABRIC-347) and write Ant scripts as receipes to do the same kinda thing.
