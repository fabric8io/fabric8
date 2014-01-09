package io.fabric8.redirect;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;

import java.util.Map;

@Component(name = "io.fabric8.redirect", label = "Fabric8 redirect servlet", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = false)
public class RedirectRegistration extends AbstractComponent {

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();

    private final RedirectServlet redirectServlet = new RedirectServlet();

    @Activate
    void activate(Map<String, ?> configuration) throws ServletException, NamespaceException {
        httpService.get().registerServlet("/*", redirectServlet, null, null);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        httpService.get().unregister("/*");
    }

    void bindHttpService(HttpService service) {
        this.httpService.bind(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.unbind(service);
    }
}
