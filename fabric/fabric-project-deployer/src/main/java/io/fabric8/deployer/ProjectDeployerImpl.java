/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.deployer;

import io.fabric8.api.Container;
import io.fabric8.api.Containers;
import io.fabric8.api.FabricRequirements;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileRequirements;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.JMXUtils;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.insight.log.support.Strings;
import io.fabric8.internal.Objects;
import io.fabric8.service.child.ChildConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.codehaus.plexus.util.IOUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Allows projects to be deployed into a profile using Jolokia / REST or build plugins such as a maven plugin
 */
@Component(name = "io.fabric8.deployer", label = "Fabric8 Project Deploy Service",
        description = "Allows projects (such as maven builds) to be deployed into a fabric profile.",
        policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ProjectDeployer.class)
public final class ProjectDeployerImpl extends AbstractComponent implements ProjectDeployer, ProjectDeployerMXBean {

    private static final transient Logger LOG = LoggerFactory.getLogger(ProjectDeployerImpl.class);
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

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private BundleContext bundleContext;
    private Map<String, String> servicemixBundles;
    private int downloadThreads;

    @Activate
    void activate(BundleContext context, Map<String, ?> configuration) throws Exception {
        bundleContext = context;
        configurer.configure(configuration, this);

        if (mbeanServer != null) {
            JMXUtils.registerMBean(this, mbeanServer, OBJECT_NAME);
        }

        loadServiceMixBundles();

        activateComponent();
    }

    @Modified
    void modified(Map<String, Object> configuration) throws Exception {
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

    @Override
    public DeployResults deployProject(ProjectRequirements requirements) throws Exception {
        Version version = getOrCreateVersion(requirements);

        // validate that all the parent profiles exists
        for (String parent : requirements.getParentProfiles()) {
            if (!version.hasProfile(parent)) {
                throw new IllegalArgumentException("Parent profile " + parent + " does not exists in version " + version.getId());
            }
        }

        Profile profile = getOrCreateProfile(version, requirements);
        boolean isAbstract = requirements.isAbstractProfile();
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        builder.addAttribute(Profile.ABSTRACT, "" + isAbstract);
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);
        profile = profileService.updateProfile(profile);

        ProjectRequirements oldRequirements = writeRequirementsJson(requirements, profile);
        updateProfileConfiguration(version, profile, requirements, oldRequirements);

        Profile overlayProfile = profileService.getOverlayProfile(profile);
        Profile effectiveProfile = Profiles.getEffectiveProfile(fabricService.get(), overlayProfile);

        Container container = null;
        try {
            container = fabricService.get().getCurrentContainer();
        } catch (Exception e) {
            // ignore
        }

        Integer minimumInstances = requirements.getMinimumInstances();
        if (minimumInstances != null) {
            FabricRequirements fabricRequirements = fabricService.get().getRequirements();
            ProfileRequirements profileRequirements = fabricRequirements.getOrCreateProfileRequirement(profile.getId());
            profileRequirements.setMinimumInstances(minimumInstances);
            fabricService.get().setRequirements(fabricRequirements);
        }
        return resolveProfileDeployments(requirements, fabricService.get(), container, profile, effectiveProfile);
    }

    /**
     * Removes any old parents / features / repos and adds any new parents / features / repos to the profile
     */
    private void updateProfileConfiguration(Version version, Profile profile, ProjectRequirements requirements, ProjectRequirements oldRequirements) {
        List<String> parentProfiles = Containers.getParentProfileIds(profile);
        List<String> bundles = profile.getBundles();
        List<String> features = profile.getFeatures();
        List<String> repositories = profile.getRepositories();
        if (oldRequirements != null) {
            removeAll(parentProfiles, oldRequirements.getParentProfiles());
            removeAll(bundles, oldRequirements.getBundles());
            removeAll(features, oldRequirements.getFeatures());
            removeAll(repositories, oldRequirements.getFeatureRepositories());
        }
        addAll(parentProfiles, requirements.getParentProfiles());
        addAll(bundles, requirements.getBundles());
        addAll(features, requirements.getFeatures());
        addAll(repositories, requirements.getFeatureRepositories());
        // Modify the profile through the {@link ProfileBuilder}
        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        setParentProfileIds(builder, version, profile, parentProfiles);
        builder.setBundles(bundles);
        builder.setFeatures(features);
        builder.setRepositories(repositories);
        String webContextPath = requirements.getWebContextPath();
        if (!Strings.isEmpty(webContextPath)) {
            Map<String, String> contextPathConfig = profile.getConfiguration(ChildConstants.WEB_CONTEXT_PATHS_PID);
            if (contextPathConfig == null) {
                contextPathConfig = new HashMap<String, String>();
            }
            String key = requirements.getGroupId() + "/" + requirements.getArtifactId();
            String current = contextPathConfig.get(key);
            if (!Objects.equal(current, webContextPath)) {
                contextPathConfig.put(key, webContextPath);
                builder.addConfiguration(ChildConstants.WEB_CONTEXT_PATHS_PID, contextPathConfig);
            }
        }
        String description = requirements.getDescription();
        if (!Strings.isEmpty(description)) {
            String fileName = "Summary.md";
            byte[] data = profile.getFileConfiguration(fileName);
            if (data == null || data.length == 0 || new String(data).trim().length() == 0) {
                builder.addConfigurationFile(fileName, description.getBytes());
            }
        }
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);
        profile = profileService.updateProfile(builder.getProfile());
    }

    /**
     * Sets the list of parent profile IDs
     */
    private void setParentProfileIds(ProfileBuilder builder, Version version, Profile profile, List<String> parentProfileIds) {
        List<Profile> list = new ArrayList<>();
        for (String parentProfileId : parentProfileIds) {
            Profile parentProfile = null;
            if (version.hasProfile(parentProfileId)) {
                parentProfile = version.getRequiredProfile(parentProfileId);
            }
            if (parentProfile != null) {
                list.add(parentProfile);
            } else {
                LOG.warn("Could not find parent profile: " + parentProfileId + " in version " + version.getId());
            }
        }
        builder.setParents(list);
    }
    
    private void addAll(List<String> list, List<String> values) {
        if (list != null && values != null) {
            for (String value : values) {
                if (!list.contains(value)) {
                    list.add(value);
                }
            }
        }
    }

    private void removeAll(List<String> list, List<String> values) {
        if (list != null && values != null) {
            list.removeAll(values);
        }
    }

    private DeployResults resolveProfileDeployments(ProjectRequirements requirements, FabricService fabric, Container container, Profile profile, Profile overlay) throws Exception {
        DependencyDTO rootDependency = requirements.getRootDependency();

        List<Feature> allFeatures = new ArrayList<Feature>();
        for (String repoUri : overlay.getRepositories()) {
            RepositoryImpl repo = new RepositoryImpl(URI.create(repoUri));
            repo.load();
            allFeatures.addAll(Arrays.asList(repo.getFeatures()));
        }

        if (rootDependency != null) {
            // as a hack lets just add this bundle in
            LOG.info("Got root: " + rootDependency);

            String bundleUrl = rootDependency.toBundleUrlWithType();

            List<String> features = new ArrayList<String>();
            List<String> bundles = new ArrayList<String>();
            List<String> optionals = new ArrayList<String>();

            if (requirements.getFeatures() != null) {
                features.addAll(requirements.getFeatures());
            }
            if (requirements.getBundles() != null) {
                bundles.addAll(requirements.getBundles());
            }

            bundles.add(bundleUrl);
            LOG.info("Adding bundle: " + bundleUrl);

            for (DependencyDTO dependency : rootDependency.getChildren()) {
                if ("test".equals(dependency.getScope()) || "provided".equals(dependency.getScope())) {
                    continue;
                }
                if ("jar".equals(dependency.getType())) {
                    String match = getAllServiceMixBundles().get(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
                    if (match != null) {
                        LOG.info("Replacing artifact " + dependency + " with servicemix bundle " + match);
                        String[] parts = match.split(":");
                        dependency.setGroupId(parts[0]);
                        dependency.setArtifactId(parts[1]);
                        dependency.setVersion(parts[2]);
                        dependency.setType("bundle");
                    }
                }
                String prefix = dependency.toBundleUrlWithoutVersion();
                List<Feature> matching = new ArrayList<>();
                for (Feature feature : allFeatures) {
                    for (BundleInfo bi : feature.getBundles()) {
                        if (!bi.isDependency() && bi.getLocation().startsWith(prefix)) {
                            matching.add(feature);
                            break;
                        }
                    }
                }
                if (matching.size() == 1) {
                    LOG.info("Found a matching feature for bundle " + dependency.toBundleUrl() + ": " + matching.get(0).getId());
                    features.add(matching.get(0).getName());
                } else {
                    LOG.info("Adding optional bundle: " + dependency.toBundleUrlWithType());
                    optionals.add(dependency.toBundleUrlWithType());
                }
            }

            // Modify the profile through the {@link ProfileBuilder}
            ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
            builder.setBundles(bundles).setOptionals(optionals).setFeatures(features);
            ProfileService profileService = fabricService.get().adapt(ProfileService.class);
            profile = profileService.updateProfile(builder.getProfile());
        }

        // lets find a hawtio profile and version
        String profileUrl = findHawtioUrl(fabric);
        if (profileUrl == null) {
            profileUrl = "/";
        }
        if (!profileUrl.endsWith("/")) {
            profileUrl += "/";
        }
        String profilePath = Profiles.convertProfileIdToPath(profile.getId());
        profileUrl += "index.html#/wiki/branch/" + profile.getVersion() + "/view/fabric/profiles/" + profilePath;
        return new DeployResults(profile, profileUrl);
    }

    /**
     * Finds a hawtio URL in the fabric
     */
    private String findHawtioUrl(FabricService fabric) {
        Container[] containers = null;
        try {
            containers = fabric.getContainers();
        } catch (Exception e) {
            LOG.debug("Ignored exception trying to find containers: " + e, e);
            return null;
        }
        for (Container aContainer : containers) {
            Profile[] profiles = aContainer.getProfiles();
            for (Profile aProfile : profiles) {
                String id = aProfile.getId();
                if (id.equals("fabric")) {
                    return fabric.profileWebAppURL("io.hawt.hawtio-web", id, aProfile.getVersion());
                }
            }
        }
        return null;
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

    // Implementation methods
    //-------------------------------------------------------------------------

    private Profile getOrCreateProfile(Version version, ProjectRequirements requirements) {
        String profileId = getProfileId(requirements);
        if (Strings.isEmpty(profileId)) {
            throw new IllegalArgumentException("No profile ID could be deduced for requirements: " + requirements);
        }
        Profile profile;
        if (!version.hasProfile(profileId)) {
            LOG.info("Creating new profile " + profileId + " version " + version + " for requirements: " + requirements);
            String versionId = version.getId();
            ProfileService profileService = fabricService.get().adapt(ProfileService.class);
            ProfileBuilder builder = ProfileBuilder.Factory.create(versionId, profileId);
            profile = profileService.createProfile(builder.getProfile());
        } else {
            profile = version.getRequiredProfile(profileId);
        }
        return profile;
    }

    private Version getOrCreateVersion(ProjectRequirements requirements) {
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);
        String versionId = getVersionId(requirements);
        Version version = findVersion(fabricService.get(), versionId);
        if (version == null) {
            String baseVersionId = requirements.getBaseVersion();
            baseVersionId = getVersionOrDefaultVersion(fabricService.get(), baseVersionId);
            Version baseVersion = findVersion(fabricService.get(), baseVersionId);
            if (baseVersion != null) {
                version = VersionBuilder.Factory.create(versionId).parent(baseVersion.getId()).getVersion();
                version = profileService.createVersion(version);
            } else {
                version = VersionBuilder.Factory.create(versionId).getVersion();
                version = profileService.createVersion(version);
            }
        }
        return version;
    }

    private Version findVersion(FabricService fabricService, String versionId) {
        ProfileService profileService = fabricService.adapt(ProfileService.class);
        return profileService.getVersion(versionId);
    }


    private String getVersionId(ProjectRequirements requirements) {
        String version = requirements.getVersion();
        return getVersionOrDefaultVersion(fabricService.get(), version);
    }

    private String getVersionOrDefaultVersion(FabricService fabricService, String versionId) {
        if (Strings.isEmpty(versionId)) {
            versionId = fabricService.getDefaultVersionId();
            if (Strings.isEmpty(versionId)) {
                versionId = "1.0";
            }
        }
        return versionId;
    }


    private String getProfileId(ProjectRequirements requirements) {
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


    private ProjectRequirements writeRequirementsJson(ProjectRequirements requirements, Profile profile) throws IOException {
        ObjectMapper mapper = DtoHelper.getMapper();
        byte[] json = mapper.writeValueAsBytes(requirements);
        String name = DtoHelper.getRequirementsConfigFileName(requirements);

        // lets read the previous requirements if there are any
        ProfileRegistry profileRegistry = fabricService.get().adapt(ProfileRegistry.class);
        String version = profile.getVersion();
        String profileId = profile.getId();
        byte[] oldData = profileRegistry.getFileConfiguration(version, profileId, name);

        LOG.info("Writing file " + name + " to profile " + profile);
        profileRegistry.setFileConfiguration(version, profileId, name, json);

        if (oldData == null || oldData.length == 0) {
            return null;
        } else {
            return mapper.reader(ProjectRequirements.class).readValue(oldData);
        }
    }

    private synchronized Map<String, String> getAllServiceMixBundles() throws InterruptedException {
        doGetAllServiceMixBundles();
        while (downloadThreads > 0) {
            wait();
        }
        return servicemixBundles;
    }

    private void loadServiceMixBundles() {
        File file = bundleContext.getDataFile("servicemix-bundles.properties");
        if (file.exists() && file.isFile()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                Map<String, String> map = new HashMap<String, String>();
                for (Enumeration e = props.propertyNames(); e.hasMoreElements(); ) {
                    String name = (String) e.nextElement();
                    map.put(name, props.getProperty(name));
                }
                long date = Long.parseLong(map.get("timestamp"));
                // cache for a day
                if (System.currentTimeMillis() - date < 24L * 60L * 60L * 1000L) {
                    servicemixBundles = map;
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        doGetAllServiceMixBundles();
    }

    private synchronized void doGetAllServiceMixBundles() {
        final ExecutorService executor = Executors.newFixedThreadPool(64);
        if (servicemixBundles != null) {
            return;
        }
        servicemixBundles = new HashMap<String, String>();
        downloadThreads++;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String md = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/").openStream());
                    Matcher matcher = Pattern.compile("<a href=\"(org\\.apache\\.servicemix\\.bundles\\.[^\"]*)/\">").matcher(md);
                    while (matcher.find()) {
                        final String artifactId = matcher.group(1);
                        synchronized (ProjectDeployerImpl.this) {
                            downloadThreads++;
                        }
                        executor.execute(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String mda = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/" + artifactId).openStream());
                                    Matcher matcher = Pattern.compile("<a href=\"([^\\.][^\"]*)/\">").matcher(mda);
                                    while (matcher.find()) {
                                        final String version = matcher.group(1);
                                        synchronized (ProjectDeployerImpl.this) {
                                            downloadThreads++;
                                        }
                                        executor.execute(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    String pom = IOUtil.toString(new URL("http://central.maven.org/maven2/org/apache/servicemix/bundles/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom").openStream());
                                                    String pkgGroupId = extract(pom, "<pkgGroupId>(.*)</pkgGroupId>");
                                                    String pkgArtifactId = extract(pom, "<pkgArtifactId>(.*)</pkgArtifactId>");
                                                    String pkgVersion = extract(pom, "<pkgVersion>(.*)</pkgVersion>");
                                                    if (pkgGroupId != null && pkgArtifactId != null && pkgVersion != null) {
                                                        String key = pkgGroupId + ":" + pkgArtifactId + ":" + pkgVersion;
                                                        synchronized (ProjectDeployerImpl.this) {
                                                            String cur = servicemixBundles.get(key);
                                                            if (cur == null) {
                                                                servicemixBundles.put(key, "org.apache.servicemix.bundles:" + artifactId + ":" + version);
                                                            } else {
                                                                int v1 = extractBundleRelease(cur);
                                                                int v2 = extractBundleRelease(version);
                                                                if (v2 > v1) {
                                                                    servicemixBundles.put(key, "org.apache.servicemix.bundles:" + artifactId + ":" + version);
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (IOException e) {
                                                    // Ignore
                                                } finally {
                                                    downloadThreadDone();
                                                }
                                            }
                                        });
                                    }
                                } catch (IOException e) {
                                    // Ignore
                                } finally {
                                    downloadThreadDone();
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    // Ignore
                } finally {
                    downloadThreadDone();
                }
            }
        });
    }

    private synchronized void downloadThreadDone() {
        if (--downloadThreads == 0) {
            File file = bundleContext.getDataFile("servicemix-bundles.properties");
            Properties props = new Properties();
            props.putAll(servicemixBundles);
            props.put("timestamp", Long.toString(System.currentTimeMillis()));
            try (FileOutputStream fos = new FileOutputStream(file)) {
                props.store(fos, "ServiceMix Bundles");
            } catch (IOException e) {
                // Ignore
            }
        }
        ProjectDeployerImpl.this.notifyAll();
    }

    private int extractBundleRelease(String version) {
        int i0 = version.lastIndexOf('_');
        int i1 = version.lastIndexOf('-');
        int i = Math.max(i0, i1);
        if (i > 0) {
            return Integer.parseInt(version.substring(i + 1));
        }
        return -1;
    }

    private String extract(String string, String regexp) {
        Matcher matcher = Pattern.compile(regexp).matcher(string);
        return matcher.find() ? matcher.group(1) : null;
    }

}
