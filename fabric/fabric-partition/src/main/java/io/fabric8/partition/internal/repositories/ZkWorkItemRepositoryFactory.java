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

import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.partition.WorkItemRepository;
import io.fabric8.partition.WorkItemRepositoryFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.url.URLStreamHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = ZkWorkItemRepositoryFactory.ID, label = "Fabric8 ZooKeeper Work Item WorkItemRepository", metatype = false)
@Service(WorkItemRepositoryFactory.class)
@org.apache.felix.scr.annotations.Properties(
        @Property(name = "type", value = ZkWorkItemRepositoryFactory.TYPE)
)
public class ZkWorkItemRepositoryFactory extends AbstractComponent implements WorkItemRepositoryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkWorkItemRepositoryFactory.class);

    public static final String TYPE = "zookeeper";
    public static final String SCHEME = "zk";
    public static final String ID = ID_PREFIX + TYPE;

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Reference(referenceInterface = URLStreamHandlerService.class, target = "url.handler.protocol=" + SCHEME)
    private final ValidatingReference<URLStreamHandlerService> urlHandler = new ValidatingReference<URLStreamHandlerService>();


    @Activate
    void activate() {
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
        return new ZkWorkItemRepository(curator.get(), path);
    }


    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    void bindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.bind(urlHandler);
    }

    void unbindUrlHandler(URLStreamHandlerService urlHandler) {
        this.urlHandler.unbind(urlHandler);
    }
}
