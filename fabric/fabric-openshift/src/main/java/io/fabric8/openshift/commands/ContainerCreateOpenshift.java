package io.fabric8.openshift.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.boot.commands.support.ContainerCreateSupport;
import io.fabric8.openshift.CreateOpenshiftContainerOptions;
import io.fabric8.utils.shell.ShellUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.fabric8.utils.FabricValidations.validateProfileName;

@Command(name = "container-create-openshift", scope = "fabric", description = "Creates one or more new containers on Openshift")
public class ContainerCreateOpenshift extends ContainerCreateSupport {

    @Option(name = "--server-url", required = false, description = "The url to the openshift server.")
    String serverUrl;

    @Option(name = "--login", required = false, description = "The login name to use.")
    String login;

    @Option(name = "--password", required = false, description = "The password to use.")
    String password;

    @Option(name = "--proxy-uri", description = "The Maven proxy URL to use")
    private URI proxyUri;

    @Argument(index = 0, required = true, description = "The name of the container to be created. When creating multiple containers it serves as a prefix")
    protected String name;

    @Argument(index = 1, required = false, description = "The number of containers that should be created")
    protected int number = 0;

    @Override
    protected Object doExecute() throws Exception {
        // validate input before creating containers
        preCreateContainer(name);
        validateProfileName(profiles);

        CreateOpenshiftContainerOptions.Builder builder = CreateOpenshiftContainerOptions.builder()
                .name(name)
                .serverUrl(serverUrl)
                .login(login)
                .password(password)
                .version(version)
                .ensembleServer(isEnsembleServer)
                .zookeeperUrl(fabricService.getZookeeperUrl())
                .zookeeperPassword(isEnsembleServer && zookeeperPassword != null ? zookeeperPassword : fabricService.getZookeeperPassword())
                .proxyUri(proxyUri != null ? proxyUri : fabricService.getMavenRepoURI())
                .profiles(getProfileNames())
                .dataStoreProperties(getDataStoreProperties())
                .dataStoreType(dataStoreType != null && isEnsembleServer ? dataStoreType : fabricService.getDataStore().getType());

        CreateContainerMetadata[] metadatas = fabricService.createContainers(builder.build());

        if (isEnsembleServer && metadatas != null && metadatas.length > 0 && metadatas[0].isSuccess()) {
            ShellUtils.storeZookeeperPassword(session, metadatas[0].getCreateOptions().getZookeeperPassword());
        }
        // display containers
        displayContainers(metadatas);
        return null;
    }

    protected void displayContainers(CreateContainerMetadata[] metadatas) {
        List<CreateContainerMetadata> success = new ArrayList<CreateContainerMetadata>();
        List<CreateContainerMetadata> failures = new ArrayList<CreateContainerMetadata>();
        for (CreateContainerMetadata metadata : metadatas) {
            (metadata.isSuccess() ? success : failures).add(metadata);
        }
        if (success.size() > 0) {
            System.out.println("The following containers have been created successfully:");
            for (CreateContainerMetadata m : success) {
                System.out.println("\t" + m.toString());
            }
        }
        if (failures.size() > 0) {
            System.out.println("The following containers have failed:");
            for (CreateContainerMetadata m : failures) {
                System.out.println("\t" + m.getContainerName() + ": " + m.getFailure().getMessage());
            }
        }
    }

}
