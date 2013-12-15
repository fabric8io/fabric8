/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.boot.commands.support;

import org.apache.felix.gogo.commands.Option;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.FabricAuthenticationException;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.ZooKeeperClusterService;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.utils.FabricValidations.validateContainersName;

public abstract class ContainerCreateSupport extends FabricCommand {
    @Option(name = "--version", description = "The version of the new container (must be an existing version). Defaults to the current default version.")
    protected String version;
    @Option(name = "--profile", multiValued = true, required = false, description = "The profile IDs to associate with the new container(s). For multiple profiles, specify the flag multiple times. Defaults to the profile named, default.")
    protected Set<String> profiles;
    @Option(name = "--resolver", multiValued = false, required = false, description = "The resolver policy for this container(s). Possible values are: localip, localhostname, publicip, publichostname, manualip. Defaults to the fabric's default resolver policy.")
    protected String resolver;
    @Option(name = "-m", aliases = {"--manual-ip"}, description = "An address to use, when using the manualip resolver.")
    protected String manualIp;
    @Option(name = "-b", aliases = {"--bind-address"}, description = "The default bind address.")
    protected String bindAddress;
    @Option(name = "--ensemble-server", multiValued = false, required = false, description = "Whether the new container should be a fabric ensemble server (ZooKeeper ensemble server).")
    protected Boolean isEnsembleServer = Boolean.FALSE;
    @Option(name = "--zookeeper-password", multiValued = false, description = "The ensemble password to use (one will be generated if not given)")
    protected String zookeeperPassword;
    @Option(name = "--jvm-opts", multiValued = false, required = false, description = "Options to pass to the container's JVM.")
    protected String jvmOpts;
    @Option(name = "--datastore-type", multiValued = false, required = false, description = "Options to pass to the container's datastore type.")
    protected String dataStoreType;
    @Option(name = "--datastore-option", multiValued = true, required = false, description = "Options to pass to the container's datastore.")
    protected String[] dataStoreOption;

    public Set<String> getProfileNames() {
        Set<String> names = this.profiles;
        if (names == null || names.isEmpty()) {
            names = new LinkedHashSet<String>();
            names.add("default");
        }
        return names;
    }

    /**
     * Pre validates input before creating the container(s)
     *
     * @param name the name of the container to create
     * @throws IllegalArgumentException is thrown if input is invalid
     */
    protected void preCreateContainer(String name) throws IllegalArgumentException {
        validateContainersName(name);
        if (!isEnsembleServer) {
            ServiceReference sr = getBundleContext().getServiceReference(ZooKeeperClusterService.class.getName());
            ZooKeeperClusterService zkcs = sr != null ? getService(ZooKeeperClusterService.class, sr) : null;
            if (zkcs == null) {
                throw new IllegalStateException("Unable to find ZooKeeperClusterService service");
            }
            if (zkcs.getEnsembleContainers().isEmpty()) {
                if (!isEnsembleServer) {
                    throw new IllegalStateException("The use of the --ensemble-server option is mandatory when creating an initial container");
                }
                return;
            }

            if (doesContainerExist(name)) {
                throw new IllegalArgumentException("A container with name " + name + " already exists.");
            }

            // get the profiles for the given version
            Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
            Profile[] profiles = ver.getProfiles();

            // validate profiles exists before creating a new container
            Set<String> names = getProfileNames();
            for (String profile : names) {
                Profile prof = getProfile(profiles, profile, ver);
                if (prof == null) {
                    throw new IllegalArgumentException("Profile " + profile + " with version " + ver.getId() + " does not exist.");
                }
                if (prof.isAbstract()) {
                    throw new IllegalArgumentException("Profile " + profile + " with version " + ver.getId() + " is abstract and can not be associated to containers.");
                }
            }
        }

        if (!isEnsembleServer && fabricService.getZookeeperUrl() == null) {
            throw new IllegalArgumentException("Either start a zookeeper ensemble or use --ensemble-server.");
        }
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

    protected void rethrowAuthenticationErrors(CreateContainerMetadata[] metadata) {
        FabricAuthenticationException rethrow = null;
        for (CreateContainerMetadata md : metadata) {
            if (md.getFailure() instanceof FabricAuthenticationException) {
                rethrow = (FabricAuthenticationException) md.getFailure();
            } else {
                return;
            }
        }
        if (rethrow != null) {
            throw rethrow;
        }
    }

    private static Profile getProfile(Profile[] profiles, String name, Version version) {
        if (profiles == null || profiles.length == 0) {
            return null;
        }

        for (Profile profile : profiles) {
            if (profile.getId().equals(name) && profile.getVersion().equals(version.getId())) {
                return profile;
            }
        }

        return null;
    }

    public Map<String, String> getDataStoreProperties() {
        Map<String, String> options = new HashMap<String, String>(fabricService.getDataStore().getDataStoreProperties());
        if (dataStoreOption != null) {
            for (String opt : dataStoreOption) {
                String[] parts = opt.trim().split(" +");
                options.put(parts[0], parts[1]);
            }
        }
        return options;
    }
}
