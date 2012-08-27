# ﻿INTRODUCTION

This RedHat OpenShift cartridge "Do it YourSelf" allows to deploy Fuse Fabric in the cloud.

# Get started

1. Get a free OpenShift account by signing up at http://openshift.redhat.com

2. Define your namespace/subdomain -->  "subdomain".rhcloud.com - https://openshift.redhat.com/community/faq/what-is-a-namespace

3. Add a new application and choose "Do it yourself" as application type

    ![../add-application1.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/add-application1.png)
    ![../add-application2.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/add-application2.png)

    ![../do-it-yourself.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/do-it-yourself.png)

3. Clone the Git Openshift project locally using the command provided

    git clone ssh://UUID@SUBDOMAIN.rhcloud.com/~/git/APPLICATION.git/
    cd

    where UUID = UUID provided by OpenShift
          SUBDOMAIN = namespace created previously
          APPLICATION = name of the application

    Remark : Do not forget to upload your public key to your openshift account : ['My Account' section of the web console](https://openshift.redhat.com/app/account)

    ![../git-ssh-info.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/git-ssh-info.png)

4. Merge the OpenShift project with the Fuse Fabric cartridge

    Copy the content of this directory (except the pom file) locally - https://github.com/fusesource/fuse/raw/master/fabric/openshift/cartridge
    Commit the Git changes to Openshift (git commit -m "Initial import of Fuse Fabric" -a & git push

5. Whenever you publish a git project to Openshift, it will be deployed and the application started.

    So you should see the following message on the screen

    git push
    Counting objects: 19, done.
    Delta compression using up to 8 threads.
    Compressing objects: 100% (10/10), done.
    Writing objects: 100% (10/10), 948 bytes, done.
    Total 10 (delta 6), reused 0 (delta 0)
    remote: Stopping application...
    remote: Exception in thread "main" java.lang.NumberFormatException: null
    remote: 	at java.lang.Integer.parseInt(Integer.java:454)
    remote: 	at java.lang.Integer.parseInt(Integer.java:527)
    remote: 	at org.apache.karaf.main.Stop.main(Stop.java:66)
    remote: Done
    remote: ~/git/fabric.git ~/git/fabric.git
    remote: ~/git/fabric.git
    remote: Running .openshift/action_hooks/pre_build
    remote: Running .openshift/action_hooks/build
    remote: Running .openshift/action_hooks/deploy
    remote: + '[' -d /var/lib/stickshift/2ee23a78e9664b8593ebbc77e16aac38/fabric/runtime//fabric ']'
    remote: + exit 0
    remote: Starting application...
    remote: + export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.5.x86_64
    remote: + JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.5.x86_64
    remote: + export PATH=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.5.x86_64/bin:/usr/libexec/stickshift/cartridges/diy-0.1/info/bin/:/usr/libexec/stickshift/cartridges/abstract/info/bin/:/sbin:/usr/sbin:/bin:/usr/bin
    remote: + PATH=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.5.x86_64/bin:/usr/libexec/stickshift/cartridges/diy-0.1/info/bin/:/usr/libexec/stickshift/cartridges/abstract/info/bin/:/sbin:/usr/sbin:/bin:/usr/bin
    remote: + export OPENSHIFT_SSH_PORT=18101
    remote: + OPENSHIFT_SSH_PORT=18101
    remote: + export OPENSHIFT_SSH_HOST=
    remote: + OPENSHIFT_SSH_HOST=
    remote: + cd /var/lib/stickshift/2ee23a78e9664b8593ebbc77e16aac38/fabric/runtime//fabric
    remote: + bin/start
    remote: Done
    remote: Running .openshift/action_hooks/post_deploy
    To ssh://2ee23a78e9664b8593ebbc77e16aac38@fabric-fuse.rhcloud.com/~/git/fabric.git/
    124aefc..273b7b3  master -> master

    REMARK : There is a issue as the port file is not created by Karaf

6. Now you can connect to the Karaf web console using the link : http://APPLICATION-SUBDOMAIN.rhcloud.com/system/console/bundles

    login : karaf
    password : karaf

7. You can connect to your cloud instance using ssh and next to karaf

    ssh UUID@APPLICATION-SUBDOMAIN.rhcloud.com

    On the cloud instance

    ssh localhost -p 18101 -l karaf -P karaf

8. Application can be stopped and restarted using these [rhc](https://openshift.redhat.com/app/getting_started) commands

    rhc app stop -a APPLICATION Application@http://APPLICATION-SUBDOMAIN.rhcloud.com
    rhc app start -a APPLICATION Application@http://APPLICATION-SUBDOMAIN.rhcloud.com


# ISSUES

    1) Until now we cannot connect to karaf using ssh port number. A thread has been created on [Red Hat Forum] (https://openshift.redhat.com/community/forums/openshift/ssh-issue).
    2) As Karaf generates an error when it stops due to missing port file under data directory, every new GIT change is not correctly propagated
      on Openshift and update of the project does not work well (manual changes are required when connected to the instance).











