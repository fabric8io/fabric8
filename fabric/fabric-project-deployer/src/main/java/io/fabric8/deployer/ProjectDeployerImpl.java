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

import static io.fabric8.agent.download.ProfileDownloader.getMavenCoords;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
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
import io.fabric8.common.util.Lists;
import io.fabric8.deployer.dto.DependencyDTO;
import io.fabric8.deployer.dto.DeployResults;
import io.fabric8.deployer.dto.DtoHelper;
import io.fabric8.deployer.dto.ProjectRequirements;
import io.fabric8.insight.log.support.Strings;
import io.fabric8.internal.Objects;
import io.fabric8.service.VersionPropertyPointerResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import io.fabric8.utils.FabricValidations;
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
    public static final String[] RESOLVER_IGNORE_BUNDLE_PREFIXES = {"org.slf4j", "log4j"};

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
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        ProjectRequirements oldRequirements = writeRequirementsJson(requirements, profile, builder);
        updateProfileConfiguration(version, profile, requirements, oldRequirements, builder);

        return resolveProfileDeployments(requirements, fabricService.get(), profile, builder);
    }

    /**
     * Removes any old parents / features / repos and adds any new parents / features / repos to the profile
     */
    private void updateProfileConfiguration(Version version, Profile profile, ProjectRequirements requirements, ProjectRequirements oldRequirements, ProfileBuilder builder) {
        List<String> parentProfiles = Lists.mutableList(profile.getParentIds());
        List<String> bundles = Lists.mutableList(profile.getBundles());
        List<String> features = Lists.mutableList(profile.getFeatures());
        List<String> repositories = Lists.mutableList(profile.getRepositories());
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
        setParentProfileIds(builder, version, profile, parentProfiles);
        builder.setBundles(bundles);
        builder.setFeatures(features);
        builder.setRepositories(repositories);
        Boolean locked = requirements.getLocked();
        if (locked != null) {
            builder.setLocked(locked);
        }
        String webContextPath = requirements.getWebContextPath();
        if (!Strings.isEmpty(webContextPath)) {
            Map<String, String> contextPathConfig = new HashMap<>();
            Map<String, String> oldValue = profile.getConfiguration(Constants.WEB_CONTEXT_PATHS_PID);
            if (oldValue != null) {
                contextPathConfig.putAll(oldValue);
            }
            String key = requirements.getGroupId() + "/" + requirements.getArtifactId();
            String current = contextPathConfig.get(key);
            if (!Objects.equal(current, webContextPath)) {
                contextPathConfig.put(key, webContextPath);
                builder.addConfiguration(Constants.WEB_CONTEXT_PATHS_PID, contextPathConfig);
            }
        }
        String description = requirements.getDescription();
        if (!Strings.isEmpty(description)) {
            String fileName = "Summary.md";
            byte[] data = profile.getFileConfiguration(fileName);
            if (data == null || data.length == 0 || new String(data).trim().length() == 0) {
                builder.addFileConfiguration(fileName, description.getBytes());
            }
        }
    }

    /**
     * Sets the list of parent profile IDs
     */
    private void setParentProfileIds(ProfileBuilder builder, Version version, Profile profile, List<String> parentProfileIds) {
        List<String> list = new ArrayList<>();
        for (String parentProfileId : parentProfileIds) {
            if (version.hasProfile(parentProfileId)) {
                list.add(parentProfileId);
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

    private DeployResults resolveProfileDeployments(ProjectRequirements requirements, FabricService fabric, Profile profile, ProfileBuilder builder) throws Exception {
        DependencyDTO rootDependency = requirements.getRootDependency();
        ProfileService profileService = fabricService.get().adapt(ProfileService.class);

        if (rootDependency != null) {
            // as a hack lets just add this bundle in
            LOG.info("Got root: " + rootDependency);
            List<String> parentIds = profile.getParentIds();
            Profile overlay = profileService.getOverlayProfile(profile);

            String bundleUrl = rootDependency.toBundleUrlWithType();
            LOG.info("Using resolver to add extra features and bundles on " + bundleUrl);

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

            // TODO we maybe should detect a karaf based container in a nicer way than this?
            boolean isKarafContainer = parentIds.contains("karaf") || parentIds.contains("containers-karaf");
            boolean addBundleDependencies = Objects.equal("bundle", rootDependency.getType()) || isKarafContainer;
            if (addBundleDependencies && requirements.isUseResolver()) {

                // lets build up a list of all current active features and bundles along with all discovered features
                List<Feature> availableFeatures = new ArrayList<Feature>();
                addAvailableFeaturesFromProfile(availableFeatures, fabric, overlay);

                Set<String> currentBundleLocations = new HashSet<>();
                currentBundleLocations.addAll(bundles);

                // lets add the current features
                DownloadManager downloadManager = DownloadManagers.createDownloadManager(fabric, executorService);
                Set<Feature> currentFeatures = AgentUtils.getFeatures(fabric, downloadManager, overlay);
                addBundlesFromProfile(currentBundleLocations, overlay);

                List<String> parentProfileIds = requirements.getParentProfiles();
                if (parentProfileIds != null) {
                    for (String parentProfileId : parentProfileIds) {
                        Profile parentProfile = profileService.getProfile(profile.getVersion(), parentProfileId);
                        Profile parentOverlay = profileService.getOverlayProfile(parentProfile);
                        Set<Feature> parentFeatures = AgentUtils.getFeatures(fabric, downloadManager, parentOverlay);
                        currentFeatures.addAll(parentFeatures);
                        addAvailableFeaturesFromProfile(availableFeatures, fabric, parentOverlay);
                        addBundlesFromProfile(currentBundleLocations, parentOverlay);
                    }
                }

                // lets add all known features from the known repositories
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
                    Feature feature = findFeatureWithBundleLocationPrefix(currentFeatures, prefix);
                    if (feature != null) {
                        LOG.info("Feature is already is in the profile " + feature.getId() + " for " + dependency.toBundleUrl() );
                    } else {
                        feature = findFeatureWithBundleLocationPrefix(availableFeatures, prefix);
                        if (feature != null) {
                            String name = feature.getName();
                            if (features.contains(name)) {
                                LOG.info("Feature is already added " + name + " for " + dependency.toBundleUrl() );
                            } else {
                                LOG.info("Found a matching feature for bundle " + dependency.toBundleUrl() + ": " + feature.getId());
                                features.add(name);
                            }
                        } else {
                            String bundleUrlWithType = dependency.toBundleUrlWithType();
                            String foundBundleUri = findBundleUri(currentBundleLocations, prefix);
                            if (foundBundleUri != null) {
                                LOG.info("Bundle already included " + foundBundleUri + " for " + bundleUrlWithType);
                            } else {
                                boolean ignore = false;
                                String bundleWithoutMvnPrefix = getMavenCoords(bundleUrlWithType);
                                for (String ignoreBundlePrefix : RESOLVER_IGNORE_BUNDLE_PREFIXES) {
                                    if (bundleWithoutMvnPrefix.startsWith(ignoreBundlePrefix)) {
                                        ignore = true;
                                        break;
                                    }
                                }
                                if (ignore) {
                                    LOG.info("Ignoring bundle: " + bundleUrlWithType);
                                } else {
                                    boolean optional = dependency.isOptional();
                                    LOG.info("Adding " + (optional ? "optional " : "") + " bundle: " + bundleUrlWithType);
                                    if (optional) {
                                        optionals.add(bundleUrlWithType);
                                    } else {
                                        bundles.add(bundleUrlWithType);
                                    }
                                }
                            }
                        }
                    }
                }
                // Modify the profile through the {@link ProfileBuilder}
                builder.setOptionals(optionals).setFeatures(features);
            }
            builder.setBundles(bundles);
        }

        profile = profileService.updateProfile(builder.getProfile());

        Integer minimumInstances = requirements.getMinimumInstances();
        if (minimumInstances != null) {
            FabricRequirements fabricRequirements = fabricService.get().getRequirements();
            ProfileRequirements profileRequirements = fabricRequirements.getOrCreateProfileRequirement(profile.getId());
            profileRequirements.setMinimumInstances(minimumInstances);
            fabricService.get().setRequirements(fabricRequirements);
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

    protected  String findBundleUri(Set<String> bundleLocations, String prefix) {
        for (String bundleLocation : bundleLocations) {
            if (bundleLocation.startsWith(prefix)) {
                return bundleLocation;
            }
        }
        return null;
    }

    protected void addBundlesFromProfile(Set<String> currentBundleUris, Profile overlay) {
        List<String> bundles = overlay.getBundles();
        if (bundles != null) {
            currentBundleUris.addAll(bundles);
        }
    }

    protected void addAvailableFeaturesFromProfile(Collection<Feature> allFeatures, FabricService fabric, Profile overlay) throws Exception {
        for (String repoUriWithExpressions : overlay.getRepositories()) {
            String repoUri = VersionPropertyPointerResolver.replaceVersions(fabric, overlay.getConfigurations(), repoUriWithExpressions);
            RepositoryImpl repo = new RepositoryImpl(URI.create(repoUri));
            repo.load();
            allFeatures.addAll(Arrays.asList(repo.getFeatures()));
        }
    }

    protected Feature findFeatureWithBundleLocationPrefix(Iterable<Feature> allFeatures, String prefix) {
        // lets try to find the feature ignoring any dependencies to try find the closest match
        Feature feature = findFeatureWithBundleLocationPrefix(allFeatures, prefix, false);
        if (feature == null) {
            feature = findFeatureWithBundleLocationPrefix(allFeatures, prefix, true);
        }
        return feature;
    }

    protected Feature findFeatureWithBundleLocationPrefix(Iterable<Feature> allFeatures, String prefix, boolean includeDependencies) {
        for (Feature feature : allFeatures) {
            Feature matchedFeature = featureMatchesBundleLocationPrefix(feature, prefix, feature, includeDependencies);
            if (matchedFeature != null) {
                return matchedFeature;
            }
        }
        return null;
    }

    /**
     * Returns the owningFeature if this feature or any of its dependent features contains a bundle matching the prefix location or null if there is no match
     */
    protected Feature featureMatchesBundleLocationPrefix(Feature feature, String prefix, Feature owningFeature, boolean includeDependencies) {
        for (BundleInfo bi : feature.getBundles()) {
            if (!bi.isDependency() && bi.getLocation().startsWith(prefix)) {
                return owningFeature;
            }
        }
        if (includeDependencies) {
            for (Feature dependency: feature.getDependencies()) {
                Feature answer = featureMatchesBundleLocationPrefix(dependency, prefix, owningFeature, true);
                if (answer != null) {
                    return answer;
                }
            }
        }
        return null;
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
        // make sure the profileId is valid
        FabricValidations.validateProfileName(profileId);

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
            String baseId = requirements.getBaseVersion();
            baseId = getVersionOrDefaultVersion(fabricService.get(), baseId);
            Version baseVersion = findVersion(fabricService.get(), baseId);
            if (baseVersion != null) {
                version = profileService.createVersion(baseVersion.getId(), versionId, null);
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


    private ProjectRequirements writeRequirementsJson(ProjectRequirements requirements, Profile profile, ProfileBuilder builder) throws IOException {
        ObjectMapper mapper = DtoHelper.getMapper();
        byte[] json = mapper.writeValueAsBytes(requirements);
        String fileName = DtoHelper.getRequirementsConfigFileName(requirements);

        // lets read the previous requirements if there are any
        ProfileRegistry profileRegistry = fabricService.get().adapt(ProfileRegistry.class);
        byte[] oldData = profile.getFileConfiguration(fileName);

        LOG.info("Writing file " + fileName + " to profile " + profile);
        builder.addFileConfiguration(fileName, json);

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
