## SSH Containers

Fabric8 allows you to install from scratch containers in the local network via ssh. Fabric will install the container from scratch and will configure the container to automatically join the fabric cluster.

### Requirements

The requirements for using this feature to create a container in a remote host are:

* **Any unix operating system**
* **SSHD running on the target host**
  * **A valid account credentials** or
  * **Configured public key authentication**
* **Java 1.6 or later installed**
* **Curl installed**
* **GNU tar installed**
* **Firewalls disabled between containers or ports below opened**

### Creating containers using the shell

Fabric provides the [fabric:container-create-ssh](commands/fabric-container-create-ssh.html) command, for creating ssh containers.

#### Examples

Assuming that we have a host with hostname *myhost* accessible from our local network, with a user account name **myuser** with password **mypassword**.
We can create a container to myhost from the shell, using the following command:

        fabric:container-create-ssh --host myhost --username myuser --password mypassword myremotecontainername

If user *myuser* on host *myhost* has configure public key authentication for the user on which we are currently logged we can skip the password option:

        fabric:container-create-ssh --host myhost --username myuser myremotecontainername

This command will make use of the ~/.ssh/id_rsa key for authentication. In case that you want to use an other key, you can specify it using the **--private-key** option:

        fabric:container-create-ssh --host myhost --username myuser --private-key ~/.ssh/fabric_pk myremotecontainername

The last command also supports the **--pass-phrase** option if your key is configured with a pass phrase.

### Creating a remote registry server via ssh

There are cases, that you don't have an existing fabric and want to create one on a remote host. The obvious option is to do it manually, but fabric allows you to create a remote fabric registry server *(ensemble-server)* using the ensemble server option.
This is extremely usefull as it can allow any devops guy setup the whole cluster, just using his local installation.

        fabric:container-create-ssh --host myhost --username myuser --ensemble-server myremotecontainername
        fabric:join myhost:2181

<object width="853" height="480"><param name="movie" value="http://www.youtube.com/v/clS_17BGgjM?version=3&amp;hl=en_US&amp;rel=0"></param><param name="allowFullScreen" value="true"></param><param name="allowscriptaccess" value="always"></param><embed src="http://www.youtube.com/v/clS_17BGgjM?version=3&amp;hl=en_US&amp;rel=0" type="application/x-shockwave-flash" width="853" height="480" allowscriptaccess="always" allowfullscreen="true"></embed></object>

### Managing remote (ssh) containers
The ssh container provider capabilities do not stop to the creation of the container. Fabric also allows you to stop, restart or delete *(uninstall)* a remote container.

To stop an *(ssh)* container:

        fabric:container-stop myremotecontainername

To restart an *(ssh)* container:

        fabric:container-start myremotecontainername

To uninstall an *(ssh)* container:

        fabric:container-delete myremotecontainername

Note that these commands are only available for containers that have been created using Fabric. This means that the are not usable on containers that have been installed and joined in the cluster manually.

### Ports
Some organisations require firewalls between containers within a Fabric.  If this is the case the following ports need to be opened for SSH containers to be created, provisioned, started and managed.

**Note** that if running more than one container on a server these port numbers will increase with each new container.  Below are the defaults so opening a port range should be considered if scaling multiple containers on a single node.

<table class="table table-striped">
<tr>
<th>Component</th>
<th>Port</th>
</tr>
<tr>
<td>Karaf</td>
<td>8101</td>
</tr>
<tr>
<td>JMX</td>
<td>44444</td>
</tr>
<tr>
<td>RMI</td>
<td>1099</td>
</tr>
<tr>
<td>Zookeeper</td>
<td>2181</td>
</tr>
<tr>
<td>Hawtio</td>
<td>8181</td>
</tr>
<tr>
<td>Jolokia</td>
<td>8181</td>
</tr>
<tr>
<td>Git</td>
<td>8181</td>
</tr>
</table>

For info, Zookeeper servers also use the following ports to communicate with each other.

<table class="table table-striped">
<tr>
<th>Operation</th>
<th>Port</th>
</tr>
<tr>
<td>Peer</td>
<td>2888</td>
</tr>
<tr>
<td>Election</td>
<td>3888</td>
</tr>
</table>
