﻿INTRODUCTION
============

This RedHat OpenShift cartridge "Do it YourSelf" allows to deploy Fuse Fabric in the cloud.

Get started
===========

1. Get a free OpenShift account by signing up at http://openshift.redhat.com

2. Define your namespace/subdomain -->  "subdomain".rhcloud.com - https://openshift.redhat.com/community/faq/what-is-a-namespace

2. Add a new application and choose "Do it yourself" as application type

![../add-application1.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/add-application1.png)
![../add-application2.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/add-application2.png)

![../do-it-yourself.png](https://github.com/fusesource/fuse/raw/master//openshift/do-it-yourself.png)

3. Clone the Git Openshift project locally using the command provided

    git clone ssh://UUID@SUBDOMAIN.rhcloud.com/~/git/APPLICATION.git/
    cd

    where UUID = UUID provided by OpenShift
          SUBDOMAIN = namespace created previously
          APPLICATION = name of the application

    Remark : Do not forget to upload your public key to your openshift account : ['My Account' section of the web console](https://openshift.redhat.com/app/account)

![../git-ssh-info.png](https://github.com/fusesource/fuse/raw/master/fabric/openshift/git-ssh-info.png)

4. Merge the OpenShift project with the Fuse Fabric cartridge








