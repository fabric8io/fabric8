/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.jolokia;

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.*;
import org.jolokia.osgi.servlet.JolokiaServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Map;

@Component(name = "io.fabric8.jolokia", label = "Fabric8 Jolokia", policy = ConfigurationPolicy.OPTIONAL, metatype = true, immediate = true)
public class FabricJolokia extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(FabricJolokia.class);

    @Property(name = "alias", label = "Servlet Alias", description = "Servlet Alias", value = "/jolokia")
    private String alias;

    @Property(name = "realm", label = "Jaas Realm", description = "Jaas Realm", value = "karaf")
    private String realm;

    @Property(name = "role", label = "Jaas Role", description = "Jaas Role", value = "admin")
    private String role;

    private HttpContext context;


    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();

    @Activate
    void activate(BundleContext bundleContext, Map<String, String> properties) throws Exception {
        configurer.configure(properties, this);
        context = new JolokiaSecureHttpContext(realm, role);
        httpService.get().registerServlet(getServletAlias(), new JolokiaServlet(bundleContext), new Hashtable(), context);
        activateComponent();

    }

    @Deactivate
    void deactivate() {
        try {
            httpService.get().unregister(getServletAlias());
        } catch (Throwable t) {
            LOGGER.warn("Error while unregistering jolokia.");
        }
    }


    public String getServletAlias() {
        return alias;
    }


    void bindHttpService(HttpService service) {
        this.httpService.bind(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.unbind(service);
    }

}
