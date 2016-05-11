## fabric8.yml file

The `fabric8.yml` file is an optional YAML file that the **Microservices Platform** tools use to store project configuration inside the source code of a project. 

Its a place you can store things like

* the path of the [Jenkins Workflow script](https://github.com/jenkinsci/workflow-plugin) from the [Jenkins Workflow Library](jenkinsWorkflowLibrary.html)
* the name of the chat room and issue tracker
* web links to all the different services for a project (git repository, jenkins build, chat room, issue tracker, staging and production environments etc)

Usually the file is lazily created when there is some custom configuration for a project to be stored.
 
### Editing the `fabric8.yml` file

There are a few ways you can edit the contents:

* the `project settings` button in the [console](console.html)
* the `fabric8.yml` file directly using a text editor in source control
* using the [Microservices Platform Forge commands from the CLI or in your IDE](forge.html)
