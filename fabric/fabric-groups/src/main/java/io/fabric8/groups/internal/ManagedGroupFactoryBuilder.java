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
package io.fabric8.groups.internal;

import org.apache.curator.framework.CuratorFramework;

import java.util.concurrent.Callable;

/**
 *
 */
public class ManagedGroupFactoryBuilder {

    public static ManagedGroupFactory create(CuratorFramework curator,
                                      ClassLoader loader,
                                      Callable<CuratorFramework> factory) throws Exception {
        if (curator != null) {
            return new StaticManagedGroupFactory(curator, false);
        }
        try {
            return new OsgiManagedGroupFactory(loader);
        } catch (NoClassDefFoundError e) {
            // Ignore if we'e not in OSGi
        } catch (IllegalStateException e) {
            // Ignore if we'e not in OSGi
        }
        return new StaticManagedGroupFactory(factory.call(), true);
    }

}
