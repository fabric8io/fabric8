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
package org.fusesource.fabric.itests.paxexam.support;

import org.fusesource.fabric.zookeeper.IZKClient;

import java.util.concurrent.Callable;

public class WaitForZookeeperUrlChange implements Callable<String> {

    private final IZKClient zookeeper;
    private final String url;
    private boolean keepRunning = true;

    public WaitForZookeeperUrlChange(IZKClient zookeeper, String url) {
        this.zookeeper = zookeeper;
        this.url = url;
    }

    @Override
    public String call() {
        while (keepRunning) {
            if (!url.equals(zookeeper.getConnectString())) {
                return zookeeper.getConnectString();
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
