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
package org.fusesource.fabric.commands.support;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.zookeeper.ZkClientFacade;
import org.fusesource.fabric.zookeeper.ZkDefs;

import java.util.List;

public class ProfileCompleter implements Completer {

    protected FabricService fabricService;
    protected ZkClientFacade zooKeeper;

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        String versionName = null;
        Version defaultVersion = fabricService.getDefaultVersion();
        if (defaultVersion != null) {
            versionName = defaultVersion.getName();
        }
        if (versionName == null) {
            versionName = ZkDefs.DEFAULT_VERSION;
        }
        Profile[] profiles = fabricService.getProfiles(versionName);
        for (Profile profile : profiles) {
            delegate.getStrings().add(profile.getId());
        }
        return delegate.complete(buffer, cursor, candidates);
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public ZkClientFacade getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZkClientFacade zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

}
