package io.fabric8.openshift.commands;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import com.openshift.client.IUser;
import com.openshift.client.cartridge.StandaloneCartridge;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "application-create", scope = "openshift", description = "Creates an application")
public class ApplicationCreateCommand extends OpenshiftCommandSupport {

    static final String FORMAT = "%-30s %s";

    @Option(name = "--domain", required = false, description = "Create applications on that domain.")
    String domainId;

    @Argument(index = 0, name = "application", required = true, description = "The target application.")
    String applicationName;

    @Argument(index = 1, name = "cartridge", required = true, multiValued = false, description = "The cartridge to use.")
    String cartridge;

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        IUser user = connection.getUser();
        IDomain domain = domainId != null ? user.getDomain(domainId) : user.getDefaultDomain();
        if (domainId != null && domain == null) {
            domain = user.createDomain(domainId);
        }

        IApplication application = domain.createApplication(applicationName, new StandaloneCartridge(cartridge));
        System.out.println(application.getCreationLog());
        return null;
    }
}
