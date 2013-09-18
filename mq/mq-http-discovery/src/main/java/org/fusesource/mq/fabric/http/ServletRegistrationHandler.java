package org.fusesource.mq.fabric.http;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.fabric.service.support.AbstractComponent;
import org.fusesource.fabric.service.support.ValidatingReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Component(name = "org.fusesource.mq.fabric.http", description = "Fabric Discovery Servlet", immediate = true)
public class ServletRegistrationHandler extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletRegistrationHandler.class);

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Activate
    synchronized void activate(ComponentContext context, Map<String, String> properties) {
        activateComponent();
        try {
            FabricDiscoveryServlet discoveryServlet = new FabricDiscoveryServlet();
            discoveryServlet.setCurator(curator.get());
            HttpContext base = httpService.get().createDefaultHttpContext();
            httpService.get().registerServlet("/mq-discovery", discoveryServlet, createParams("mq-discovery"), base);
        } catch (Throwable t) {
            deactivateComponent();
            LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
        }
    }

    @Deactivate
    synchronized void deactivate() {
        try {
            try {
                httpService.get().unregister("/mq-discovery");
            } catch (Exception ex) {
                LOGGER.warn("Http service returned error on servlet unregister. Possibly the service has already been stopped");
            }
        } finally {
            deactivateComponent();
        }
    }

    private Dictionary createParams(String name) {
        Dictionary d = new Hashtable();
        d.put("servlet-name", name);
        return d;
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.set(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.set(null);
    }

    void bindHttpService(HttpService service) {
        this.httpService.set(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.set(null);
    }
}