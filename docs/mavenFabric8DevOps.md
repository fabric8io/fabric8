## fabric8:devops

The maven `fabric8:devops` goal uses the [fabric8-devops-connector](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-devops-connector) to connect various DevOps services like git hosting, [chat](chat.html), issue tracking and [CI / CD builds in jenkins](cdelivery.html) for a project reusing the optional project specific `fabric8.yml` configuration file

## Example

If you are inside a project and you have logged into your kubernetes environment (such as via the [fabric8 vagrant image](getStarted/vagrant.html) then you can type:

```
mvn fabric8:devops
```

Then the following happens:

* any `fabric8.yml` file in the project is parsed
* the project and build is added to the current kubernetes namespace with links to the available jenkins builds, chat room, issue tracker and team page

This maven goal could be fired by a Jenkins seed build whenever the source or the `fabric8.yml` file is edited.
