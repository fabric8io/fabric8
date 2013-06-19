package org.fusesource.process.fabric.commands;

import org.apache.felix.gogo.commands.Option;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class ContainerInstallSupport extends ContainerProcessCommandSupport {

    @Option(name="-c", aliases={"--controllerUrl"}, required = false, description = "The optional JSON document URL containing the controller configuration")
    protected String controllerJson;
    @Option(name="-k", aliases={"--kind"}, required = false, description = "The kind of controller to create")
    protected String controllerKind;

    protected URL getControllerURL() throws MalformedURLException {
        URL controllerUrl = null;
        if (controllerJson != null) {
            controllerUrl = new URL(controllerJson);
        } else if (controllerKind != null) {
            String name = controllerKind + ".json";
            controllerUrl = new URL("profile:" + name);
            if (controllerUrl == null) {
                throw new IllegalStateException("Cannot find controller kind: " + name + " on the classpath");
            }
        }
        return controllerUrl;
    }

}
