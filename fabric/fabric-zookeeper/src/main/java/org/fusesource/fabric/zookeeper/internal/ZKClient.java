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

package org.fusesource.fabric.zookeeper.internal;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.zookeeper.Watcher;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZooKeeperFactory;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;

@Deprecated
public class ZKClient extends AbstractZKClient {

    public ZKClient(String connectString, Timespan sessionTimeout, Watcher watcher) {
        super(connectString, sessionTimeout, watcher);
        Map<String, String> acls = new HashMap<String, String>();
        acls.put("/", "world:anyone:acdrw");
        setACLs(acls);
    }

    public ZKClient(IZooKeeperFactory factory) {
        super(factory);
    }

    public ZKClient(IZooKeeperFactory factory, String chroot) {
        super(factory, chroot);
    }

    @Override
    protected void doStart() throws InvalidSyntaxException, ConfigurationException, UnsupportedEncodingException {
        connect();
    }

}
