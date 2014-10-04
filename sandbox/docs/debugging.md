## Debugging Containers

When you start to use lots of containers in fabric8 you will probably need debug them. Most IDEs like eclipse and IDEA have remote debugger support; the challenge is enabling debug and figuring out which host and port the containers are listening on.

### Enable debugging for a container

Some containers come out of the box with debugging enabled by default (e.g. Tomcat).

Others you can customise this via an environment variable. For example with Karaf based containers you define **KARAF_DEBUG** to be true. e.g. on a unix

    export KARAF_DEBUG=true

For the [Java Containers](javaContainer.html) and [Spring Boot](springBootContainer.html) there is a debug profile called **containers-debug**; just add that profile to your container and it'll enable debug.

### Finding the host and port to connect to

To perform a remote debug in your IDE you need to find the host and port to connect the debugger to. Once you have these values you should be able to use the wizard in your IDE to setup a _remote debug* session.

### Web Console

If you are using the web console then:

* open the **Container page** for the container you want to debug (e.g. click on the container name on the containers page)
* open the **URLs** tab
* the debug port and debug host name should appear in the top of the form
* you can click on the _Copy to clipboard_ button on the right of each field to copy the values to the clipboard for easy pasting into your IDE.

#### Command line

In the fabric8 command line shell type:

    container-info mycontainer

Where _mycontainer_ is the name of the container you wish to debug. The debug port and host should appear on the console.

