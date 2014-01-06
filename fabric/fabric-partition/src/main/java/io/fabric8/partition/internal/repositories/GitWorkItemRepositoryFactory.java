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
package io.fabric8.partition.internal.repositories;

import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.git.internal.GitDataStore;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.WorkItemRepositoryFactory;
import io.fabric8.utils.SystemProperties;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = GitWorkItemRepositoryFactory.ID, description = "Git Work Item WorkItemRepository")
@Service(WorkItemRepositoryFactory.class)
public class GitWorkItemRepositoryFactory extends AbstractComponent implements WorkItemRepositoryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitWorkItemRepositoryFactory.class);

    public static final String TYPE = "git";
    public static final String ID = ID_PREFIX + TYPE;

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();

    @Reference(referenceInterface = DataStore.class, target = "(type=caching-git)")
    private final ValidatingReference<GitDataStore> dataStore = new ValidatingReference<GitDataStore>();

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = URLStreamHandlerService.class, target = "url.handler.protocol=profile")
    private final ValidatingReference<URLStreamHandlerService> urlHandler = new ValidatingReference<URLStreamHandlerService>();

    private String name;


    @Activate
    void activate() {
        RuntimeProperties props = runtimeProperties.get();
        name = props.getProperty(SystemProperties.KARAF_NAME);
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public WorkItemRepository build(String path) {
        assertValid();
        return new GitWorkItemRepository(name, dataStore.get(), path, fabricService.get());
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindDataStore(DataStore dataStore) {
        if (dataStore instanceof GitDataStore) {
            this.dataStore.bind((GitDataStore) dataStore);
        }
    }

    void unbindDataStore(DataStore dataStore) {
        if (dataStore instanceof GitDataStore) {
            this.dataStore.unbind((GitDataStore) dataStore);
        }
    }

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    void bindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.bind(urlHandler);
    }

    void unbindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.unbind(urlHandler);
    }

}

