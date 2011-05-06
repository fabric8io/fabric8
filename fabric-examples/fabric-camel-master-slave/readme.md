The purpose of this example/demo is to demonstrate the provisioning feature of fabric
and how we can use the fabric-camel component in a cluster environment where we deploy a part of
the Camel route on one instance and the other on a separate

When deploying the example/demo we will show you the following features of fabric with
 Zookeeper like How :
- Create two karaf instances, let's say the root is the master and the slave is a karaf test.
- Define a profile using fabric
- Assign the profile to the karaf test instance (= agent)
- Deploy a fabric-camel project on the master
- Deploy a fabric-camel project on the agent

To run the demo, execute the following instructions :

1) Download and install a Karaf 2.2.1-SNAPSHOT
2) Modify the /etc/startup.properties script to use Apache Felix ConfigAdmin 1.2.9-SNAPSHOT
3) Add this bundle into the /system/org/apache/felix/org.apache.felix.configadmin/org.apache.felix.configadmin-1.2.9-SNAPSHOT.jar

4) Create a text file install-fabric-camel.txt containing the following instructions

# install Fabric-camel + camel example in 2.2.1 or later of Apache Karaf

features:addurl mvn:org.fusesource.fabric/fabric-distro/1.0-SNAPSHOT/xml/features

features:install fabric-commands
shell:echo Waiting a couple of seconds for the fabric-commands to start
shell:sleep 5000

fabric:zk-cluster root
shell:echo Waiting a couple of seconds for the zookeeper server to start
shell:sleep 5000

shell:echo Installing camel features repository and deploy camel
features:addurl mvn:org.apache.camel.karaf/apache-camel/2.7.0/xml/features
features:install camel
shell:sleep 5000

shell:echo Installing camel fabric demo features repository and deploy camel-master
features:addurl mvn:org.fusesource.fabric.examples.fabric-camel-master-slave/features/1.0-SNAPSHOT/xml/features
features:install camel-master
shell:sleep 5000

shell:echo Create fabric profile
fabric:create-profile --parents default fabric-test

shell:echo add repositories, features to be deployed in the store
zk:create -r /fabric/configs/versions/base/profiles/fabric-test/org.fusesource.fabric.agent/repository.camel mvn:org.apache.camel.karaf/apache-camel/2.7.0/xml/features
zk:create -r /fabric/configs/versions/base/profiles/fabric-test/org.fusesource.fabric.agent/feature.camel camel

zk:create -r /fabric/configs/versions/base/profiles/fabric-test/org.fusesource.fabric.agent/repository.fabric-camel-test mvn:org.fusesource.fabric.examples.fabric-camel-master-slave/features/1.0-SNAPSHOT/xml/features
zk:create -r /fabric/configs/versions/base/profiles/fabric-test/org.fusesource.fabric.agent/feature.fabric-camel-test camel-slave

shell:echo Create fabric agent
fabric:create-agent --profile fabric-test --parent root test
shell:sleep 1000

5) Save the file somewhere into your file system
6) Start karaf --> bin/karaf.sh or bin/karaf.bat
7) Execute the shell command
shell:source file:///pathToYourFile/install-fabric-camel.txt

example --> shell:source file:////Users/charlesmoulliard/Fuse/fabric/fabric-examples/fabric-camel-master-slave/install-fabric-camel.txt

8) Then you will see the following messages on the console

Waiting a couple of seconds for the fabric-commands to start
Waiting a couple of seconds for the zookeeper server to start
Installing camel features repository and deploy camel
Installing camel fabric demo features repository and deploy camel-master
Create fabric profile
add repositories, features to be deployed in the store
Create fabric agent
Creating new instance on SSH port 8102 and RMI ports 1100/44445 at: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test
Creating dir:  /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/bin
Creating dir:  /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc
Creating dir:  /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/system
Creating dir:  /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/deploy
Creating dir:  /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/data
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/config.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/jre.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/custom.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/java.util.logging.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.apache.felix.fileinstall-deploy.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.apache.karaf.log.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.apache.karaf.features.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.ops4j.pax.logging.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.ops4j.pax.url.mvn.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/startup.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/users.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/system.properties
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.apache.karaf.shell.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/etc/org.apache.karaf.management.cfg
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/bin/karaf
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/bin/start
Creating file: /Users/charlesmoulliard/Applications/apache-karaf-2.2.1-SNAPSHOT/instances/test/bin/stop


9) Connect to the agent
fabric:connect test

REMARK : If you cannot connect to the agent, here is the workarounb

admin:stop test
admin:start --wait test
fabric:connect test

10) Check that the camel fabric route can consume messages from camel master !

13:10:47,566 | INFO  | foo              | fabric-slave                     | ache.camel.processor.CamelLogger  196 | 59 - org.apache.camel.camel-core - 2.7.0 | >> Response from Master to Slave : Hello Boys

and that master receive message from slave

13:09:27,564 | INFO  | qtp1461085307-94 | fabric-master                    | ache.camel.processor.CamelLogger  196 | 58 - org.apache.camel.camel-core - 2.7.0 | Request received : Hello from Fabric Slave

Enjoy the first fabric-camel demo (Created the 06th May 2011) !
