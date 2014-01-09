package org.fusesource.mq.fabric.http;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
@Component(name = "org.fusesource.mq.fabric.http", label = "Fabric8 Discovery Servlet", immediate = true, metatype = true)
public final class ServletRegistrationHandler extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletRegistrationHandler.class);

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Activate
    void activate(ComponentContext context, Map<String, ?> properties) {
        try {
            FabricDiscoveryServlet discoveryServlet = new FabricDiscoveryServlet();
            discoveryServlet.setCurator(curator.get());
            HttpContext base = httpService.get().createDefaultHttpContext();
            httpService.get().registerServlet("/mq-discovery", discoveryServlet, createParams("mq-discovery"), base);
        } catch (Throwable t) {
            LOGGER.warn("Failed to register fabric maven proxy servlets, due to:" + t.getMessage());
        }
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        try {
            httpService.get().unregister("/mq-discovery");
        } catch (Exception ex) {
            LOGGER.warn("Http service returned error on servlet unregister.");
        }
    }

    private Dictionary<String, String> createParams(String name) {
        Dictionary<String, String> d = new Hashtable<String, String>();
        d.put("servlet-name", name);
        return d;
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindHttpService(HttpService service) {
        this.httpService.bind(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.unbind(service);
    }
}