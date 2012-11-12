== Fuse Management Console ${project.version} ==

===================================================
                   IMPORTANT!
===================================================
Before using the Fuse Management Console you must edit:

etc/fmc-users.properties

to create a user account.

Otherwise you will be unable to log into the management
console.
===================================================

To start the Fuse Management Console (FMC) run the following
command in a terminal:

UNIX
----

./bin/fmc

Windows
-------

.\bin\fmc.bat


To start FMC in the background:

UNIX
----

./bin/fmc-start

Windows
-------

.\bin\fmc-start.bat


Once FMC is started open a supported web browser and access the
console at the following URL:

http://localhost:${fmc-port}

Follow the guide on the welcome page to either create a new
Fabric Ensemble Server or join an existing Fabric Ensemble.

Then log in using the username and password you've configured
in etc/fmc-users.properties

For more information about Fuse Management Console please
visit:

http://fusesource.com/products/fuse-management-console/#documentation
