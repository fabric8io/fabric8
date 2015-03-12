# Forge REST Service

This project provides a REST service implemented via JAX-RS for interacting with Forge so that web based tooling can be created.

### Building

[Install maven](http://maven.apache.org/download.cgi) then run:

    mvn install
    cd main
    mvn install exec:java

The REST API should be running now at [http://localhost:8599/](http://localhost:8599/) which should list some APIs you can invoke to try it out.


### HTML5 Console

The [hawtio](http://hawt.io) project has a HTML5 plugin for [Forge](http://forge.jboss.org/) called [hawtio-forge](https://github.com/hawtio/hawtio-forge) to interact with Forge from your web browser using this REST API.

For more details check out the [hawtio-forge documentation](https://github.com/hawtio/hawtio-forge/blob/master/ReadMe.md)

#### Invoking commands

To invoke a command **my-command* POST an ExecutionRequest as JSON to the URI **http://localhost:8599/api/forge/commands/my-command** of the form:

```
{
  "resource": "someSelectedFolderOrFileName",
  "items":  {
    "foo": "bar",
  },
  "promptQueue": ["something"]
}
```

Some commands don't require any input; usually though the selected resource is required; particularly for project related commands which need to know the folder of the project.

#### List the Camel components on a project

Try this if you are running the REST service as described above:

    curl -H "Content-Type: application/json" -d '{"resource":"../examples/example-camel-cdi"}' http://localhost:8588/api/forge/commands/project-camel-component-list


