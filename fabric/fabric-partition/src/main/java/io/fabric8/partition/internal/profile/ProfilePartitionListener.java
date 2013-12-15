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

package io.fabric8.partition.internal.profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.curator.utils.ZKPaths;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.GuardedBy;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.partition.Partition;
import io.fabric8.partition.PartitionListener;
import org.mvel2.ParserContext;
import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;
import org.osgi.service.component.annotations.Activate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;

@ThreadSafe
@Component(name = "io.fabric8.partition.listener.profile", description = "Fabric Profile Partition Listener", immediate = true)
@Service(PartitionListener.class)
public final class ProfilePartitionListener extends AbstractComponent implements PartitionListener {

    private static final String ID = "id";
    private static final String TYPE = "profile-template";
    private static final String NAME_VARIABLE_FORMAT = "__%s__";

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @GuardedBy("this") private final Map<Key, CompiledTemplate> templates = new HashMap<Key, CompiledTemplate>();
    @GuardedBy("this") private final SetMultimap<String, Partition> assignedPartitons = HashMultimap.<String, Partition>create();
    @GuardedBy("this") private final ParserContext parserContext = new ParserContext();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        destroyInternal();
        deactivateComponent();
    }

    private synchronized void destroyInternal() {
        for (String taskDefinition : assignedPartitons.keySet()) {
            stop(null, taskDefinition, assignedPartitons.get(taskDefinition));
        }
    }

    @Override
    public String getType() {
        assertValid();
        return TYPE;
    }

    @Override
    public synchronized void start(String taskId, String taskDefinition, Set<Partition> partitions) {
        assertValid();
        Container current = fabricService.get().getCurrentContainer();
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
            current.addProfiles(targetProfile);
            assignedPartitons.put(taskDefinition, partition);
        }
    }

    @Override
    public synchronized void stop(String taskId, String taskDefinition, Set<Partition> partitions) {
        assertValid();
        Container current = fabricService.get().getCurrentContainer();
        Version version = current.getVersion();
        for (Partition partition : partitions) {
            String profileId = taskDefinition + "-" + ZKPaths.getNodeFromPath(partition.getId());
            Profile toBeRemoved = fabricService.get().getVersion(version.getId()).getProfile(profileId);
            current.removeProfiles(toBeRemoved);
            assignedPartitons.remove(taskDefinition, partition);
        }
    }

    private String renderTemplateName(String name, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            name = name.replaceAll(String.format(NAME_VARIABLE_FORMAT, entry.getKey()), entry.getValue());
        }
        return name.substring(0, name.lastIndexOf("."));
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
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
