# Migration

## Migrating profiles from the old Fabric8 instances

If you try to migrate the older version of Fabric (let's say old
[JBoss Fuse 6.0](http://www.jboss.org/products/fuse/overview) instance) to the new Fabric8 installation, you will
notice that there is no `fabric8:zip` Maven command available for your old project. Maven tooling has been introduced
in the more recent versions of the Fabric8, so you can't use the Maven plugin to export your profiles from the older
Fabric8 instances.

In order to migrate profiles from your older Fabric8 installations to the brand new Fabric8 instance, you need to:

**Step 1**: Install new Fabric8 server:

    unzip fabric8-karaf-1.1.0-SNAPSHOT.zip
    mv fabric8-karaf-1.1.0-SNAPSHOT /opt/new-fabric8-home

**Step 2**:Export the registry of your old Fabric8 instance using the `fabric:export` shell command

    > fabric:export
    Export to /opt/old-fabric8-home/fabric/export completed successfully

**Step 3**: Copy the profiles you want to migrate into the `fabric/import/fabric/configs/versions` directory in your new Fabric
instance

     % cp -r /opt/old-fabric8-home/fabric/configs/versions/1.0/profiles/{foo,bar}  /opt/new-fabric8-home/fabric/import/fabric/configs/versions/1.0/profiles/

**Step 4**: Start new Fabric server

     % /opt/new-fabric8-home/bin/fabric8

The `foo` and `bar` profiles will be automatically imported from the `new-fabric8-home/fabric/import` directory:

    > profile-list | grep 'foo|bar'
    foo                         0              default
    bar                         0              default
