package org.fusesource.mq.fabric.http;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

/**
 */
@Component(name = "org.fusesource.mq.fabric.http",
        description = "Fabric Discovery Servlet",
        immediate = true)
public class ServletRegistrationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletRegistrationHandler.class);

    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private HttpService httpService;
    @Reference(cardinality = org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY)
    private CuratorFramework curator;

    @Activate
    public void init(Map<String, String> properties) throws IOException {
        try {
            FabricDiscoveryServlet discoveryServlet = new FabricDiscoveryServlet();
            discoveryServlet.setCurator(curator);
            HttpContext base = httpService.createDefaultHttpContext();
            httpService.registerServlet("/mq-discovery", discoveryServlet, createParams("mq-discovery"), base);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
        }
    }

    private Dictionary createParams(String name) {
        Dictionary d = new Hashtable();
        d.put("servlet-name", name);
        return d;
    }

    @Deactivate
    public void destroy() {
        try {
            if (httpService != null) {
                httpService.unregister("/mq-discovery");
            }
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
        }
    }

}
