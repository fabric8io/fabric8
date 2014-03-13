## Rolling Upgrades

Fabric8 is designed to make it easy to manage groups of containers as if they were a single container; just update the _profile_ once and all the containers update in real time to match the new configuration, software components, versions, patches etc.

However sometimes its better to take a more incremental approach to making configuration changes. So to perform a rolling upgrade you

* create a new version (which in Fuse 6.1 or later just really means a new branch of the git configuration repository)
* make changes to whatever profiles you like
* choose one or more containers to roll forward to the new version
* wait and see how it behaves
* roll forward more containers
* if anything goes bad, roll the containers back to the previous version

You can then do things like schedule when containers should roll forward to new versions; or make the rolling of container versions asynchronous to when the changes in configuration are made. It then decouples the changing of configuration from the changes becoming active.
