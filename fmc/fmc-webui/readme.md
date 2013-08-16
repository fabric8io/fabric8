___  ___ _____
|  \/  |/  __ \
| .  . || /  \/
| |\/| || |
| |  | || \__/\
\_|  |_/ \____/
_______________

This is the Management Console WAR project, this readme is pretty much for copy/pasting commands.

To watch changes to coffeescript and jade files do:

* mvn brew:compile -Dbrew.watch=true

in the fmc-webui directory. If you make changes to the backend stuff (any of the scala, etc) you can rebuild using:

* mvn compile war:exploded bundle:manifest

which will rebuild the exploded war file.

Note that since the bundle is fully redeployed whenever a file in the exploded war is updated you'll run into OOM PermGen issues, so might be worth bumping that up.

* JAVA_OPTS="-XX:PermSize=64m -XX:MaxPermSize=512m ./bin/fmc

With debug enabled:

* JAVA_OPTS="-XX:PermSize=64m -XX:MaxPermSize=512m -Xmx2G -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005" ./bin/fmc

