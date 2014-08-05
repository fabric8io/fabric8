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
package io.fabric8.agent.commands.metadata;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.jmx.MetaTypeAttributeDTO;
import io.fabric8.api.jmx.MetaTypeObjectDTO;
import io.fabric8.api.jmx.MetaTypeObjectSummaryDTO;
import io.fabric8.api.jmx.MetaTypeSummaryDTO;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.JMXUtils;
import io.fabric8.common.util.Objects;
import org.apache.felix.metatype.AD;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.fabric8.api.jmx.MetaTypeAttributeDTO.typeName;


/**
 * A service to determine the OSGi MetaType information for a profile which is not yet running
 * by discoverying the OSGi MetaType XML files inside the bundles of a profile
 */
@Component(name = "io.fabric8.agent.commands.metadata.ProfileMetadata",
        label = "Fabric8 Profile Metadata Service",
        policy = ConfigurationPolicy.OPTIONAL,
        immediate = true, metatype = true)
public class ProfileMetadata extends AbstractComponent implements ProfileMetadataMXBean {
    protected static String PROPERTIES_SUFFIX = ".properties";
    protected static String XML_SUFFIX = ".xml";

    private static final Logger LOG = LoggerFactory.getLogger(ProfileMetadata.class);

    private static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ProfileMetadata");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Property(name = "metaTypeFolder", value = "${runtime.home}/metatype",
            label = "Metatype Directory",
            description = "Directory containing the MetaType metadata files")
    private File metaTypeFolder;

    @Reference
    private Configurer configurer;

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Activate
    void activate(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
        activateComponent();
        if (mbeanServer != null) {
            StandardMBean mbean = new StandardMBean(this, ProfileMetadataMXBean.class);
            JMXUtils.registerMBean(mbean, mbeanServer, OBJECT_NAME);
        }
    }


    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        updateConfiguration(configuration);
    }

    private void updateConfiguration(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
    }

    @Deactivate
    void deactivate() throws Exception {
        deactivateComponent();
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
    }


    @Override
    public MetaTypeSummaryDTO metaTypeSummary(String versionId, String profileId) throws Exception {
        final MetaTypeSummaryDTO answer = new MetaTypeSummaryDTO();
        MetadataHandler handler = new MetadataHandler() {
            @Override
            public void invoke(MetaData metadata, Properties resources) {
                addMetaData(answer, metadata, resources);
            }
        };
        findMetadataForProfile(versionId, profileId, handler);
        return answer;
    }

    protected void findMetadataForProfile(String versionId, String profileId, MetadataHandler handler) throws Exception {
        FabricService service = fabricService.get();
        Objects.notNull(service, "FabricService");

        ProfileService profileService = service.adapt(ProfileService.class);
        Objects.notNull(profileService, "ProfileService");

        DownloadManager downloadManager = DownloadManagers.createDownloadManager(service, executorService);
        Objects.notNull(downloadManager, "DownloadManager");

        Profile immediateProfile = profileService.getProfile(versionId, profileId);
        Objects.notNull(immediateProfile, "Profile for versionId: " + versionId + ", profileId: " + profileId);
        Profile profile = profileService.getOverlayProfile(immediateProfile);

        Set<String> pids = new HashSet<>();
        Map<String, File> fileMap = AgentUtils.downloadProfileArtifacts(service, downloadManager, profile);
        Set<Map.Entry<String, File>> entries = fileMap.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String uri = entry.getKey();
            File file = entry.getValue();
            if (!file.exists() || !file.isFile()) {
                LOG.warn("File " + file + " is not an existing file for " + uri + ". Ignoring");
                continue;
            }
            addMetaTypeInformation(handler, uri, file);
            pids.add(uri);
        }

        // lets check if the MetaType folder exists
        if (metaTypeFolder != null && metaTypeFolder.exists() && metaTypeFolder.isDirectory()) {
            Set<String> configurationFileNames = profile.getConfigurationFileNames();
            for (String configName : configurationFileNames) {
                if (configName.endsWith(PROPERTIES_SUFFIX) && configName.indexOf('/') < 0) {
                    String pid = configName.substring(0, configName.length() - PROPERTIES_SUFFIX.length());
                    if (pid.length() > 0) {
                        if (pids.add(pid)) {
                            File pidFolder = new File(metaTypeFolder, pid);
                            File xmlFile = new File(pidFolder, "metatype.xml");
                            File propertiesFile = new File(pidFolder, "metatype.properties");
                            addMetaTypeInformation(handler, xmlFile, propertiesFile);
                        }
                    }
                }
            }
        }
    }

    @Override
    public MetaTypeObjectDTO getPidMetaTypeObject(String versionId, String profileId, final String pid) throws Exception {
        final AtomicReference<MetaTypeObjectDTO> answer = new AtomicReference<>(null);
        MetadataHandler handler = new MetadataHandler() {
            @Override
            public void invoke(MetaData metadata, Properties resources) {
                Map<String, Object> map = metadata.getDesignates();
                Map<String,Object> objects = metadata.getObjectClassDefinitions();
                Set<Map.Entry<String, Object>> entries = map.entrySet();
                for (Map.Entry<String, Object> entry : entries) {
                    String aPid = entry.getKey();
                    Object value = objects.get(aPid);
                    if (Objects.equal(pid, aPid) && value instanceof OCD) {
                        OCD ocd = (OCD) value;
                        answer.set(createMetaTypeObjectDTO(resources, ocd));
                    }
                }
            }
        };
        findMetadataForProfile(versionId, profileId, handler);
        return answer.get();
    }

    protected static MetaTypeObjectDTO createMetaTypeObjectDTO(Properties resources, OCD ocd) {
        MetaTypeObjectDTO answer = new MetaTypeObjectDTO();
        answer.setId(ocd.getID());
        answer.setName(localize(resources, ocd.getName()));
        answer.setDescription(localize(resources, ocd.getDescription()));

        List<MetaTypeAttributeDTO> attributeList = new ArrayList<>();

        Map<String,Object> attributes = ocd.getAttributeDefinitions();
        Set<Map.Entry<String, Object>> entries = attributes.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof AD) {
                AD ad = (AD) value;
                MetaTypeAttributeDTO attributeDTO = createMetaTypeAttributeDTO(resources, ocd, name, ad);
                if (attributeDTO != null) {
                    attributeList.add(attributeDTO);
                }
            }
        }
        answer.setAttributes(attributeList);
        return answer;
    }

    protected static MetaTypeAttributeDTO createMetaTypeAttributeDTO(Properties resources, OCD ocd, String name, AD ad) {
        MetaTypeAttributeDTO answer = new MetaTypeAttributeDTO();
        answer.setId(ad.getID());
        answer.setName(localize(resources, ad.getName()));
        answer.setDescription(localize(resources, ad.getDescription()));
        answer.setCardinality(ad.getCardinality());
        answer.setDefaultValue(ad.getDefaultValue());
        answer.setOptionLabels(ad.getOptionLabels());
        answer.setOptionValues(ad.getOptionValues());
        answer.setRequired(ad.isRequired());
        answer.setTypeName(typeName(ad.getType()));
        return answer;
    }

    protected void addMetaTypeInformation(MetadataHandler handler, File xmlFile, File propertiesFile) throws IOException {
        if (!xmlFile.exists()) {
            LOG.info("Warning! " + xmlFile + " does not exist so no OSGi MetaType metadata");
            return;
        }
        MetaDataReader reader = new MetaDataReader();
        MetaData metadata = reader.parse(new FileInputStream(xmlFile));
        // lets try get the i18n properties
        Properties properties = new Properties();
        if (propertiesFile.exists() && propertiesFile.isFile()) {
            properties.load(new FileInputStream(propertiesFile));
        }
        handler.invoke(metadata, properties);
    }

    protected void addMetaTypeInformation(MetadataHandler handler, String uri, File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        Map<String,MetaData> metadataMap = new HashMap<>();
        Map<String,Properties> propertiesMap = new HashMap<>();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("OSGI-INF/metatype/")) {
                if (name.endsWith(XML_SUFFIX)) {
                    MetaDataReader reader = new MetaDataReader();
                    InputStream in = jarFile.getInputStream(entry);
                    if (in != null) {
                        MetaData metadata = reader.parse(in);
                        if (metadata != null) {
                            String pid = name.substring(0, name.length() - XML_SUFFIX.length());
                            metadataMap.put(pid, metadata);
                        }
                    }
                } else if (name.endsWith(PROPERTIES_SUFFIX)) {
                    String pid = name.substring(0, name.length() - PROPERTIES_SUFFIX.length());
                    Properties properties = new Properties();
                    InputStream in = jarFile.getInputStream(entry);
                    if (in != null) {
                        properties.load(in);
                        propertiesMap.put(pid, properties);
                    }
                }
            }
        }
        Set<Map.Entry<String, MetaData>> metadataEntries = metadataMap.entrySet();
        for (Map.Entry<String, MetaData> metadataEntry : metadataEntries) {
            String pid = metadataEntry.getKey();
            MetaData metadata = metadataEntry.getValue();
            Properties properties = propertiesMap.get(pid);
            if (properties == null) {
                properties = new Properties();
            }
            handler.invoke(metadata, properties);
        }
    }

    protected void addMetaData(MetaTypeSummaryDTO summary, MetaData metadata, Properties resources) {
        Map<String, Object> map = metadata.getDesignates();
        Map<String,Object> objects = metadata.getObjectClassDefinitions();
        Set<Map.Entry<String, Object>> entries = map.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String pid = entry.getKey();
            Object value = objects.get(pid);
            if (value instanceof OCD) {
                OCD ocd = (OCD) value;
                MetaTypeObjectSummaryDTO object = summary.getOrCreateMetaTypeSummaryDTO(pid);
                object.setId(pid);
                object.setName(localize(resources, ocd.getName()));
                object.setDescription(localize(resources, ocd.getDescription()));
            }
        }
    }

    protected static String localize(Properties resources, String string) {
        if (string != null && string.startsWith("%") && resources != null) {
            string = string.substring(1);
            try {
                return resources.getProperty(string);
            } catch (Exception e) {
                LOG.warn("localize: Failed getting resources '" + string + "'", e);
            }
        }
        return string;
    }


    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    void unbindMBeanServer(MBeanServer mbeanServer) {
        this.mbeanServer = null;
    }
}
