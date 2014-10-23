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
package io.fabric8.zookeeper.curator;

import org.apache.curator.framework.CuratorFramework;

/**
 * Locate the {@link CuratorFramework}
 *
 * @since 10-Dec-2013
 */
public final class CuratorFrameworkLocator {

    private static CuratorFramework instance;

    // Hide ctor
    private CuratorFrameworkLocator() {
    }

    /**
     * Get the current CuratorFramework or null.
     */
    public static CuratorFramework getCuratorFramework() {
        return instance;
    }

    public static void bindCurator(CuratorFramework curator) {
        instance = curator;
    }

    public static void unbindCurator(CuratorFramework curator) {
        instance = null;
    }
}
