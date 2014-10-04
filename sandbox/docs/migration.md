## Migration

### Importing profiles from the old Fabric8 instances

If you try to migrate the older version of Fabric (let's say old
[JBoss Fuse 6.0](http://www.jboss.org/products/fuse/overview) instance) to the new Fabric8 installation, you will
notice that there is no `fabric8:zip` Maven command available for your old project. Maven tooling has been introduced
in the more recent versions of the Fabric8, so you can't use the Maven plugin to export your profiles from the older
Fabric8 instances.

In order to migrate profiles from your older Fabric8 installations to the brand new Fabric8 instance, you need to:

**Step 1**: Install new Fabric8 server.

    unzip fabric8-karaf-1.2.0-SNAPSHOT.zip
    mv fabric8-karaf-1.2.0-SNAPSHOT /opt/new-fabric8-home
    
Do not start new Fabric8 server until you reach **step 4** of the migration. Keep in mind however that your old Fabric8
server can be up and running when you export configuration from it.
    
**Step 2**: Export the registry of your old Fabric8 instance using the `fabric:export` shell command.

    > fabric:export
    Export to /opt/old-fabric8-home/fabric/export completed successfully

**Step 3**: Copy the profiles you want to migrate into the `fabric/import/fabric/configs/versions` directory in your new Fabric
instance.

For example to copy the `foo` and `bar` profiles (in Fabric version `1.0`) you can execute the following shell command:

     % cp -r /opt/old-fabric8-home/fabric/configs/versions/1.0/profiles/{foo,bar}  /opt/new-fabric8-home/fabric/import/fabric/configs/versions/1.0/profiles/

**Step 4**: Start new Fabric server.

     % /opt/new-fabric8-home/bin/fabric8

The `foo` and `bar` profiles we copied in the previous step will be automatically imported from the `new-fabric8-home/fabric/import` directory:

    > profile-list | grep 'foo|bar'
    foo                         0              default
    bar                         0              default

### Development activities

There are a number of development activities that you will need to carry out when migrating your solutions to later versions.  For migrations of [old JBoss Fuse 6.0](http://www.jboss.org/products/fuse/overview) to Fabric8 or newer versions of JBoss Fuse then below are a few steps to be aware of.

**Step 1**: Refactor old package names.

References to the fabric packages need changing from `org.fusesource.fabric` to `io.fabric8`.  This will affect dependency injection contexts, OSGi manifests and java classes in your project.  A SED script that ignores a `./git` dir could look like this:

	find . -path ./.git -prune -o -type f -name '*.xml' -o -name '*.java'  | xargs sed -b 's/org.fusesource.fabric/io.fabric8/g'

NOTE: OSX ignores the -b arg so a simple search and replace might be better

**Step 2**: Some feature names have changed

Here are a few of them...

	camel -> feature-camel
	activemq-camel -> camel-amq
	mq-fabric -> camel-amq

**Step 3**: Refactor [fabric camel](https://access.redhat.com/site/documentation/en-US/Red_Hat_JBoss_Fuse/6.1/html/Apache_Camel_Component_Reference/files/Fabric.html) component to use Apache curator

If you use the fabric camel component then you will need to use the new Curator OSGi service rather than the old org.fusesource.fabric.zookeeper.IZKClient / io.fabric8.zookeeper.IZKClient.

	<reference id="curator" interface="org.apache.curator.framework.CuratorFramework"/>

    <bean id="fabric-camel" class="io.fabric8.camel.FabricComponent">
        <property name="curator" ref="curator"/>
    </bean>

**Step 4**: Configuration

There is a better way to add configuration to a fabric now that Git is used.  We can clone the Git repo, commit changes and push them back rather than processing lots of profile-edit commands to manage PIDs or broker.xml.  This can all be scripted in such a way..

	cd ..
	rm -rf fabric

	git clone -b 1.0 http://admin:admin@localhost:8181/git/fabric
	cd fabric

	cp /${SOME_LOCATION}/mq-base/my-broker.xml ./fabric/profiles/mq/base.profile/
	cp /${SOME_LOCATION}/my-profile/*.properties ./fabric/profiles/my/profile.profile/
	cp /${SOME_LOCATION}/my-mq-profile/*.properties ./fabric/profiles/my/mq/profile.profile/

	git add *
	git commit -a -m "Add configuration"
	git push origin HEAD:1.0

This is favoured to the previous

	fabric:import 

way to add configuration to a fabric.

**Step 5**: Client connection to broker

In order to connect to a broker group it used to be enough to use `fabric://(discover:my_group_name)` but now we need to assign the profile that gets created as part of mq-create.  This is all documented in the Broker Client section.

**Step 6**: New OSGi resolver

There is a new OSGi resolver used so you may see some of these when previously they did not prevent bundles starting..

	ResolutionException: Uses constraint violation. Unable to resolve resource my-service [my-service/1.0.1] because it is exposed to package 'com.mypackage' from resources com.a and com.b via two dependency chains.

There are a few options here

  1. Ensure the packages exported are of the same version.
  2. Ensure the import of that package uses a fixed version rather than a range.
  3. Create an uber jar to wrap the dependencies and gain control over the package exports.  An example can be found [here](https://github.com/rawlingsj/issue-test-cases)
