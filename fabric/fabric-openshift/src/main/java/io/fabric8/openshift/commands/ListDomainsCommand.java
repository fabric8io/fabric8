package io.fabric8.openshift.commands;

import com.openshift.client.IDomain;
import com.openshift.client.IOpenShiftConnection;
import org.apache.felix.gogo.commands.Command;

@Command(name = "domain-list", scope = "openshift", description = "Lists available openshift domains")
public class ListDomainsCommand extends OpenshiftCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        IOpenShiftConnection connection = getOrCreateConnection();
        System.out.println("[id]");
        for (IDomain domain :connection.getDomains()){
            System.out.println(domain.getId());
        }
        return null;
    }
}
