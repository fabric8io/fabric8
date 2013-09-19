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
package org.fusesource.fabric.api;



/**
 * An interface that describes DataStore plugins.
 * The plugins are published in the OSGi registry using this interface, so that the {@link DataStoreRegistrationHandler}
 * can register the appropriate {@link DataStore} in the Service Registry based on its configuration.
 */
public interface DataStorePlugin<T extends DataStore> {

    /**
     * Return the DataStore name.
     * @return
     */
    String getType();

    /**
     * Return the {@link DataStore} instance this plugin provides.
     * @return
     */
    T getDataStore();
}
