package io.fabric8.openshift.commands;

import com.openshift.client.IApplication;
import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

@Command(name = "application-list", scope = "openshift", description = "Lists available openshift application")
public class ListApplicationsCommand extends OpenshiftCommandSupport {

    static final String FORMAT = "%-30s %s";

    @Option(name = "--domain", required = false, description = "Show only applications of that domain.")
    String domainId;

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        System.out.println(String.format(FORMAT, "[domain]", "[application id]"));

        for (IDomain domain : connection.getDomains()) {
            if (domainId == null || domainId.equals(domain.getId())) {
                String displayDomain = domain.getId();
                domain.refresh();
                for (IApplication application : domain.getApplications()) {
                    System.out.println(String.format(FORMAT, displayDomain, application.getName()));
                    displayDomain = "";
                }
            }
        }
        return null;
    }
}
