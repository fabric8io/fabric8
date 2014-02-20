/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.deployer;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.mvn.Parser;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jmx.JMXUtils;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.internal.Objects;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.insight.log.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Allows projects to be deployed into a profile using Jolokia / REST or build plugins such as a maven plugin
 */
@Component(name = "io.fabric8.deployer", label = "Fabric8 Project Deploy Service",
        description = "Allows projects (such as maven builds) to be deployed into a fabric profile.",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public final class ProjectDeployer extends AbstractComponent implements ProjectDeployerMXBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectDeployer.class);
    public static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ProjectDeployer");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);

        if (mbeanServer != null) {
            JMXUtils.registerMBean(this, mbeanServer, OBJECT_NAME);
        }
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
    }

    @Deactivate
    void deactivate() throws Exception {
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
        deactivateComponent();
    }

    @Override
    public DeployResults deployProjectJson(String requirementsJson) throws Exception {
        ProjectRequirements requirements = DtoHelper.getMapper().readValue(requirementsJson, ProjectRequirements.class);
        Objects.notNull(requirements, "ProjectRequirements");
        return deployProject(requirements);
    }

    public DeployResults deployProject(ProjectRequirements requirements) throws Exception {
        FabricService fabric = getFabricService();
        Profile profile = getOrCreateProfile(requirements);

        writeRequirementsJson(requirements, profile);
        Profile overlay = profile.getOverlay();

        DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, overlay, executorService);
        Map<String, Parser> profileArtifacts = AgentUtils.getProfileArtifacts(downloadManager, overlay);

        return resolveProfileDeployments(requirements, profile, profileArtifacts);
    }

    protected DeployResults resolveProfileDeployments(ProjectRequirements requirements, Profile profile, Map<String, Parser> profileArtifacts) {
        DependencyDTO rootDependency = requirements.getRootDependency();

        if (rootDependency != null) {
            // as a hack lets just add this bundle in
            LOG.info("Got root: " + rootDependency);

            String bundleUrl = rootDependency.toBundleUrl();
            List<String> bundles = profile.getBundles();
            // TODO remove old versions!

            String prefix = rootDependency.toBundleUrlWithoutVersion();
            List<String> originalBundles = new ArrayList<String>(bundles);
            for (String bundle : originalBundles) {
                if (bundle.startsWith(prefix)) {
                    bundles.remove(bundle);
                    if (!bundle.equals(bundleUrl)) {
                        LOG.info("Removing old version " + bundle);
                    }
                }
            }
            bundles.add(bundleUrl);
            profile.setBundles(bundles);
            LOG.info("Adding bundle: " + bundleUrl);

            // TODO deploy to the maven repo...
        }
        LOG.info("Got profile artifacts: " + profileArtifacts);
        return new DeployResults(profile);
    }


    // Properties
    //-------------------------------------------------------------------------
    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    public FabricService getFabricService() {
        return fabricService.get();
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    protected Profile getOrCreateProfile(ProjectRequirements requirements) {
        FabricService fabric = getFabricService();
        String versionId = getVersionId(requirements);
        Version version = null;
        try {
            version = fabric.getVersion(versionId);
        } catch (Exception e) {
            LOG.debug("Ignoring error looking up version; it probably doesn't exist yet: " + e, e);
        }
        if (version == null) {
            version = fabric.createVersion(versionId);
        }
        String profileId = getProfileId(requirements);
        if (Strings.isEmpty(profileId)) {
            throw new IllegalArgumentException("No profile ID could be deduced for requirements: " + requirements);
        }
        if (!version.hasProfile(profileId)) {
            version.createProfile(profileId);
            LOG.info("Creating new profile " + profileId + " version " + version + " for requirements: " + requirements);
        } else {
            LOG.info("Upadting profile " + profileId + " version " + version + " for requirements: " + requirements);
        }
        Profile profile = version.getProfile(profileId);
        Objects.notNull(profile, "Profile could not be created");
        return profile;
    }


    protected String getVersionId(ProjectRequirements requirements) {
        FabricService fabric = getFabricService();
        String version = requirements.getVersion();
        if (Strings.isEmpty(version)) {
            Version defaultVersion = fabric.getDefaultVersion();
            if (defaultVersion != null) {
                version = defaultVersion.getId();
            }
        }
        if (Strings.isEmpty(version)) {
            version = "1.0";
        }
        return version;
    }


    protected String getProfileId(ProjectRequirements requirements) {
        FabricService fabric = getFabricService();
        String profileId = requirements.getProfileId();
        if (Strings.isEmpty(profileId)) {
            // lets generate a project based on the group id / artifact id
            String groupId = requirements.getGroupId();
            String artifactId = requirements.getArtifactId();
            if (Strings.isEmpty(groupId)) {
                profileId = artifactId;
            }
            if (Strings.isEmpty(artifactId)) {
                profileId = groupId;
            } else {
                profileId = groupId + "-" + artifactId;
            }
        }
        return profileId;
    }


    protected void writeRequirementsJson(ProjectRequirements requirements, Profile profile) throws IOException {
        byte[] json = DtoHelper.getMapper().writeValueAsBytes(requirements);
        StringBuilder builder = new StringBuilder("modules/");
        String groupId = requirements.getGroupId();
        if (!Strings.isEmpty(groupId)) {
            builder.append(groupId);
            builder.append("/");
        }
        String artifactId = requirements.getArtifactId();
        if (!Strings.isEmpty(artifactId)) {
            builder.append(artifactId);
            builder.append("-");
        }
        builder.append("requirements.json");
        String name = builder.toString();
        LOG.info("Writing file " + name + " to profile " + profile);
        getFabricService().getDataStore().setFileConfiguration(profile.getVersion(), profile.getId(), name, json);
    }

}