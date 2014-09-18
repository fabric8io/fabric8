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
package io.fabric8.itests.paxexam.support;

import java.util.concurrent.Callable;

import org.apache.curator.framework.CuratorFramework;

public class WaitForZookeeperUrlChange implements Callable<String> {

    private final CuratorFramework curator;
    private final String url;
    private boolean keepRunning = true;

    public WaitForZookeeperUrlChange(CuratorFramework curator, String url) {
        this.curator = curator;
        this.url = url;
    }

    @Override
    public String call() {
        while (keepRunning) {
            if (!url.equals(curator.getZookeeperClient().getCurrentConnectionString())) {
                return curator.getZookeeperClient().getCurrentConnectionString();
            } else {
                try {
                    Thread.sleep(1000L);
                } catch (Exception ex) {
                    //
                }
            }
        }
        return null;
    }
}
