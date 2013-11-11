package org.fusesource.fabric.redirect;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.ServletException;
import java.util.Map;

@Component(name = "org.fusesource.fabric.redirect", description = "Fabric redirect servlet", immediate = true)
public class RedirectRegistration extends AbstractComponent {

    @Reference(referenceInterface = HttpService.class)
    private final ValidatingReference<HttpService> httpService = new ValidatingReference<HttpService>();

    private final RedirectServlet redirectServlet = new RedirectServlet();

    @Activate
    void activate(ComponentContext context, Map<String, ?> properties) throws ServletException, NamespaceException {
        httpService.get().registerServlet("/", redirectServlet, null, null);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
        httpService.get().unregister("/");
    }

    void bindHttpService(HttpService service) {
        this.httpService.bind(service);
    }

    void unbindHttpService(HttpService service) {
        this.httpService.unbind(service);
    }
}
