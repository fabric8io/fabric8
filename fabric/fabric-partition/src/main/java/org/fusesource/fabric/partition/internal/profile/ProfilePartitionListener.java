/*
 * Copyright 2010 Red Hat, Inc.
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

package org.fusesource.fabric.partition.internal.profile;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.apache.curator.utils.ZKPaths;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.partition.Partition;
import org.fusesource.fabric.partition.PartitionListener;
import org.fusesource.fabric.partition.internal.LoggingPartitionListener;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProfilePartitionListener implements PartitionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPartitionListener.class);

    private static final String ID = "id";
    private static final String TYPE = "profile-template";
    private static final String NAME_VARIABLE_FORMAT = "__%s__";

    private ConcurrentMap<Key, CompiledTemplate> templates = new ConcurrentHashMap<Key, CompiledTemplate>();
    private SetMultimap<String, Partition> assignedPartitons = Multimaps.synchronizedSetMultimap(HashMultimap.<String, Partition>create());

    private final ParserContext parserContext = new ParserContext();
    private final BundleContext bundleContext;
    private final Dictionary properties = new Properties();

    private ServiceRegistration registration;
    private FabricService fabricService;

    public ProfilePartitionListener(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        this.properties.put("type", TYPE);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public synchronized void init() {

    }

    @Override
    public synchronized void destroy() {
        if (fabricService != null) {
            for (String taskDefinition : assignedPartitons.keySet()) {
                stop(null, taskDefinition, assignedPartitons.get(taskDefinition));
            }
            fabricService = null;
        }

        if (registration != null) {
            registration.unregister();
        }
    }

    public synchronized void unbind(FabricService fabricService) {
        destroy();
    }

    public synchronized void bind(FabricService fabricService) {
        this.fabricService = fabricService;
        this.registration = bundleContext.registerService(PartitionListener.class.getName(), this, properties);
    }


    @Override
    public synchronized void start(String taskId, String taskDefinition, Set<Partition> partitions) {
        if (fabricService == null) {
            LOGGER.warn("Cannot start {}. Fabric Service is unavailable.", taskDefinition);
        }
        Container current = fabricService.getCurrentContainer();
        Version version = current.getVersion();
        Profile templateProfile = version.getProfile(taskDefinition);
        Iterable<String> fileTemplates = Iterables.filter(templateProfile.getFileConfigurations().keySet(), new MvelPredicate());

        for (String fileTemplate : fileTemplates) {
            Key key = new Key(templateProfile.getId(), fileTemplate);
            CompiledTemplate template = templates.get(key);
            if (template == null) {
                template = TemplateCompiler.compileTemplate(new String(templateProfile.getFileConfigurations().get(fileTemplate)), parserContext);
                templates.put(key, template);
            }
        }

        for (Partition partition : partitions) {
            Map<String, String> partitionData = partition.getData();
            if (!partitionData.containsKey(ID)) {
                partitionData.put(ID, ZKPaths.getNodeFromPath(partition.getId()));
            }
            String id = partitionData.get(ID);
            String profileId = taskDefinition + "-" + id;
            version.copyProfile(templateProfile.getId(), profileId, true);
            Profile targetProfile = version.getProfile(profileId);
            Map<String, byte[]> configs = templateProfile.getFileConfigurations();

            for (String fileTemplate : fileTemplates) {
                String file = renderTemplateName(fileTemplate, partitionData);
                Key key = new Key(templateProfile.getId(), fileTemplate);
                String renderedTemplate = TemplateRuntime.execute(templates.get(key), parserContext, partitionData).toString();
                configs.put(file, renderedTemplate.getBytes());
                configs.remove(fileTemplate);
            }

            targetProfile.setFileConfigurations(configs);
            Set<Profile> localProfiles = Sets.newHashSet(current.getProfiles());
            localProfiles.add(targetProfile);
            current.setProfiles(localProfiles.toArray(new Profile[localProfiles.size()]));
            assignedPartitons.put(taskDefinition, partition);
        }
    }

    @Override
    public synchronized void stop(String taskId, String taskDefinition, Set<Partition> partitions) {
        if (fabricService == null) {
            LOGGER.warn("Cannot stop {}. Fabric Service is unavailable.", taskDefinition);
        }
        Container current = fabricService.getCurrentContainer();
        Version version = current.getVersion();
        for (Partition partition : partitions) {
            String profileId = taskDefinition + "-" + ZKPaths.getNodeFromPath(partition.getId());
            Profile toBeRemoved = fabricService.getVersion(version.getId()).getProfile(profileId);
            Set<Profile> localProfiles = Sets.newHashSet(current.getProfiles());
            localProfiles.remove(toBeRemoved);
            current.setProfiles(localProfiles.toArray(new Profile[localProfiles.size()]));
            assignedPartitons.remove(taskDefinition, partition);
            toBeRemoved.delete();
        }
    }

    private String renderTemplateName(String name, Map<String, String> properties) {
        String result = name;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            name = name.replaceAll(String.format(NAME_VARIABLE_FORMAT, entry.getKey()), entry.getValue());
        }
        return name.substring(0, name.lastIndexOf("."));
    }

    private static class Key {
        private final String profile;
        private final String configName;

        private Key(String profile, String configName) {
            this.profile = profile;
            this.configName = configName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key that = (Key) o;

            if (configName != null ? !configName.equals(that.configName) : that.configName != null) return false;
            if (profile != null ? !profile.equals(that.profile) : that.profile != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = profile != null ? profile.hashCode() : 0;
            result = 31 * result + (configName != null ? configName.hashCode() : 0);
            return result;
        }
    }
}
