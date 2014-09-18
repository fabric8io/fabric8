## Security

This section describes how authentication and authorization works in fabric and how zookeeper is secured from external access.

Fabric8 containers are using JAAS for authentication and authorization. To make sure that all users, credentials and roles are shared between all containers and can be centrally managed, fabric is using the zookeeper to store user info.
JAAS will be used for the following cases:

* SSH access
* JMX access
* HTTP access

All Karaf-based containers out of the box are using a JAAS realm named "karaf". When fabric is created a second "karaf" realm is created that is backed by zookeeper that has higher ranking than the existing one.

### Managing users
When a fabric is created using the fabric:create command, the user will be prompted to create a new user. The user and password (encrypted) information will be stored in side zookeeper and can be shared by all fabric containers.

      fabric:create
      No user found in etc/users.properties or specified as an option. Please specify one ...
      New user name: **admin**
      Password for admin:
      Verify password for admin:

Please note, that the command will first attempt to find one or more users inside etc/users.properties. If no users is found there (default behavior) only then it will prompt you for creating a new user.

The users can be also managed after fabric has been created using the jaas commands.

      jaas:realms
      Index Realm                Module Class
          1 karaf                org.apache.karaf.jaas.modules.properties.PropertiesLoginModule
          2 karaf                org.apache.karaf.jaas.modules.publickey.PublickeyLoginModule
          3 karaf                io.fabric8.jaas.ZookeeperLoginModule

There are 3 JAAS realms all named karaf. You can choose which realm you want to manage by either specifying the index or the login module assigned to the realm:

      jaas:manage --index 3 --realm karaf

or
      jaas:manage --module io.fabric8.jaas.ZookeeperLoginModule --realm karaf

Then you can add, edit or delete users:

      jaas:useradd newuser newuserpassword
      jaas:roleadd newuser newuserrole
      jaas:update

Changes will only take effect only after the jaas:update command.

### Fabric operations that require authentication / authorization
When a user tries to create a child container, fabric will need to connect to the parent container via jmx in order to create the child instance. As mentioned above JMX access requires authentication/authorization.
Even though the user information are stored in the registry, its not possible to retrieve the user and decrypt its password in order. So the user will be prompted for a jmx user and jmx password instead.

Also when a users tries to connect to a container shell via fabric:connect, fabric will perform an ssh to the target containers shell, which again requires a username and password.

To avoid prompting the user for credentials all the time, fabric will "remember" the credentials and try to reuse them. The credentials will be stored in the command session and will be available to the user throughout the session.
Of course, its always possible to override the "cached" credentials by specifying the appropriate option. See the commands help for more info.

### Securing access to Zookeeper
To prevent unauthorized access to zookeeper, fabric will use digest authentication for zookeeper. The authdata used are fabric:<zookeeper password>.

Wait! What is the zookeeper password?

When fabric is created and prompts you for creating a new user, that users password will be used as a zookeeper password. The same applies to the case of importing users from etc/users.properties (for the first imported user). Also its possible to explicitly provide a zookeeper password:

      fabric:create --zookeeper-password mypassword

or you can even ask fabric to generate a password:

      fabric:create --generate-zookeeper-password
      Generated zookeeper password:ZLu4huXnVNqo7L0P

Regardless of how the zookeeper password has been created, its can be retrieve using the fabric:ensemble-password command:

      fabric:ensemble-password
      ZLu4huXnVNqo7L0P

You will need to have this password handy when manually joining containers to an existing fabric:

     fabric:join --zookeeper-password ZLu4huXnVNqo7L0P host:port

The ensemble password can be also needed when connecting external tools to the zookeeper registry. For example if you want to connect the FuseIDE Zooekeeper explorer to your fabric you'll need to specify the following:

* Scheme: digest
* Password: fabric:<zookeeper password>




