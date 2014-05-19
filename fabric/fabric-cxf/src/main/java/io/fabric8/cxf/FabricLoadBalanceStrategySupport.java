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
package io.fabric8.cxf;

import io.fabric8.groups.internal.ZooKeeperGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.fabric8.groups.Group;
import io.fabric8.groups.GroupListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FabricLoadBalanceStrategySupport implements LoadBalanceStrategy {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalanceStrategySupport.class);
    protected Group<CxfNodeState> group;
    protected List<String> alternateAddressList = new CopyOnWriteArrayList<String>();

    public void setGroup(final Group<CxfNodeState> group) throws Exception {
        this.group = group;
        group.add(new GroupListener<CxfNodeState>() {
            @Override
            public void groupEvent(Group<CxfNodeState> group, GroupEvent event) {
                onUpdate(group);
            }
        });
        if (group instanceof ZooKeeperGroup) {
            // trigger refresh synchronously so we have up to date state before being used by clients
            ((ZooKeeperGroup) group).clearAndRefresh(true, true);
        }
    }

    protected void onUpdate(Group<CxfNodeState> group) {
        alternateAddressList.clear();
        for (CxfNodeState node : group.members().values()) {
            if (node.services != null) {
                for (String url : node.services) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Added the CXF endpoint address " + url);
                    }
                    alternateAddressList.add(url);
                }
            }
        }
    }
    
    public Group<CxfNodeState> getGroup() {
        return group;
    }

    public List<String> getAlternateAddressList() {
        return new ArrayList<String>(alternateAddressList);
    }

}
