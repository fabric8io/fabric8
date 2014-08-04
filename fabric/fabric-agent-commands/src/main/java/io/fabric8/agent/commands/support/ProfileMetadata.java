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
package io.fabric8.agent.commands.support;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.agent.utils.AgentUtils;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.jmx.MetaTypeObjectSummaryDTO;
import io.fabric8.api.jmx.MetaTypeSummaryDTO;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.JMXUtils;
import io.fabric8.common.util.Objects;
import org.apache.felix.metatype.MetaData;
import org.apache.felix.metatype.MetaDataReader;
import org.apache.felix.metatype.OCD;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;


/**
 * A service to determine the OSGi MetaType information for a profile which is not yet running
 * by discoverying the OSGi MetaType XML files inside the bundles of a profile
 */
@Component(name = "io.fabric8.agent.commands.support.ProfileMetadata",
        label = "Fabric8 Profile Metadata Service",
        immediate = true, metatype = false)
public class ProfileMetadata extends AbstractComponent implements ProfileMetadataMXBean {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileMetadata.class);

    private static ObjectName OBJECT_NAME;

    static {
        try {
            OBJECT_NAME = new ObjectName("io.fabric8:type=ProfileMetadata");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
    @Reference(referenceInterface = MBeanServer.class, bind = "bindMBeanServer", unbind = "unbindMBeanServer")
    private MBeanServer mbeanServer;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Activate
    void activate(BundleContext bundleContext) throws Exception {
        activateComponent();
        if (mbeanServer != null) {
            StandardMBean mbean = new StandardMBean(this, ProfileMetadataMXBean.class);
            JMXUtils.registerMBean(mbean, mbeanServer, OBJECT_NAME);
        }

    }

    @Deactivate
    void deactivate(BundleContext bundleContext) throws Exception {
        deactivateComponent();
        if (mbeanServer != null) {
            JMXUtils.unregisterMBean(mbeanServer, OBJECT_NAME);
        }
    }


    @Override
    public MetaTypeSummaryDTO metaTypeSummary(String versionId, String profileId) throws Exception {
        MetaTypeSummaryDTO answer = new MetaTypeSummaryDTO();
        FabricService service = fabricService.get();
        Objects.notNull(service, "FabricService");

        ProfileService profileService = service.adapt(ProfileService.class);
        Objects.notNull(profileService, "ProfileService");

        DownloadManager downloadManager = DownloadManagers.createDownloadManager(service, executorService);
        Objects.notNull(downloadManager, "DownloadManager");

        Profile immediateProfile = profileService.getProfile(versionId, profileId);
        Objects.notNull(immediateProfile, "Profile for versionId: " + versionId + ", profileId: " + profileId);
        Profile profile = profileService.getOverlayProfile(immediateProfile);

        Map<String, File> fileMap = AgentUtils.downloadProfileArtifacts(service, downloadManager, profile);
        Set<Map.Entry<String, File>> entries = fileMap.entrySet();
        for (Map.Entry<String, File> entry : entries) {
            String uri = entry.getKey();
            File file = entry.getValue();
            if (!file.exists() || !file.isFile()) {
                LOG.warn("File " + file + " is not an existing file for " + uri + ". Ignoring");
                continue;
            }
            addMetaTypeInformation(answer, service, uri, file);
        }
        return answer;
    }

    protected void addMetaTypeInformation(MetaTypeSummaryDTO summary, FabricService service, String uri, File file) throws IOException {
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("OSGI-INF/metatype/")) {
                if (name.endsWith(".xml")) {
                    MetaDataReader reader = new MetaDataReader();
                    InputStream in = jarFile.getInputStream(entry);
                    if (in != null) {
                        MetaData metadata = reader.parse(in);
                        // lets try get the i18n properties
                        Properties properties = new Properties();
                        String propertiesFile = name.substring(0, name.length() - 3) + "properties";
                        ZipEntry propertiesEntry = jarFile.getEntry(propertiesFile);
                        if (propertiesEntry != null) {
                            InputStream propertiesIn = jarFile.getInputStream(entry);
                            if (propertiesIn != null) {
                                properties.load(propertiesIn);
                            }
                        }
                        addMetaData(summary, metadata, properties);
                    }
                }
            }
        }
    }

    protected void addMetaData(MetaTypeSummaryDTO summary, MetaData metadata, Properties resources) {
        Map<String,Object> objectClassDefinitions = metadata.getObjectClassDefinitions();
        Set<Map.Entry<String, Object>> entries = objectClassDefinitions.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String pid = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof OCD) {
                OCD ocd = (OCD) value;
                MetaTypeObjectSummaryDTO object = summary.getOrCreateMetaTypeSummaryDTO(pid);
                object.setId(pid);
                object.setName(localize(resources, ocd.getName()));
                object.setDescription(localize(resources, ocd.getDescription()));
            }
        }
    }

    protected String localize(Properties resources, String string) {
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
