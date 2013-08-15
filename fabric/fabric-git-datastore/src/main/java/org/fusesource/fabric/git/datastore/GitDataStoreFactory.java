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
package org.fusesource.fabric.git.datastore;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.fabric.api.DataStore;
import org.fusesource.fabric.api.DataStoreFactory;
import org.fusesource.fabric.git.FabricGitService;

/**
 * Factory of {@link ZooKeeperDataStore}
 */
@Component(name = "org.fusesource.fabric.git.datastore.factory",
           description = "Fabric Git DataStore Factory")
@Service(DataStoreFactory.class)
public class GitDataStoreFactory implements DataStoreFactory {
    public DataStore createDataStore() {
        return new GitDataStore();
    }
}
