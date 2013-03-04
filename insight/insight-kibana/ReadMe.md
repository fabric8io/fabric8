## Running Kibana in Fuse Fabric

Right now we've forked Kibana so its easy to run inside Fabric; hopefully we can push more of those changes back to the project.

Also we've an alternative back end using a straight Servlet rather than the ruby/JRuby back end.

### Running inight-kibana

Start a Fuse Fabric distro. Then type

    fabric:create
    fabric:container-add-profile root kibana

That will create a fabric, and modify the root container to run the **insight** kibana profile.

You should now be able to browse the web app at: http://localhost:8181/kibana/index.html

Enjoy!
