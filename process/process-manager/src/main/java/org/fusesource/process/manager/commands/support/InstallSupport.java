/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.process.manager.commands.support;

import org.apache.felix.gogo.commands.Option;

import java.net.MalformedURLException;
import java.net.URL;

/**
 */
public abstract class InstallSupport extends ProcessCommandSupport {
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
            controllerUrl = getBundleContext().getBundle().getResource(name);
            if (controllerUrl == null) {
                throw new IllegalStateException("Cannot find controller kind: " + name + " on the classpath");
            }
        }
        return controllerUrl;
    }
}
