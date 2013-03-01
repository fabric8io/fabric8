## Running Kibana in Fuse Fabric

Right now we've forked Kibana so its easy to run inside Fabric; hopefully we can push more of those changes back to the project.

Also we've an alternative back end using a straight Servlet rather than the ruby/JRuby back end.

### Running inight-kibana

Start a Fuse Fabric distro. Then type

    fabric:create

    fabric:profile-edit --features war fabric
    container-create-child --profile insight root insightOne
    install -s mvn:org.fusesource.insight/insight-kibana/99-master-SNAPSHOT

That will create a fabric, enable the WAR feature then create a child container running the **insight** profile. Finally we'll run the insight-kibana web app.

You should now be able to browse the web app at: http://localhost:8181/kibana/index.html

Enjoy!
