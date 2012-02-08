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
package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class FabricLoadBalanceStrategySupport implements LoadBalanceStrategy {
    private static final transient Log LOG = LogFactory.getLog(FabricLoadBalanceStrategySupport.class);
    protected Group group;
    protected List<String> alternateAddressList = new CopyOnWriteArrayList<String>();

    public void setGroup(final Group group) {
        this.group = group;
        group.add(new ChangeListener(){
            @Override
            public void changed() {

                alternateAddressList.clear();
                for (byte[] uri : group.members().values()) {
                    try {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Added the CXF endpoint address " + new String(uri, "UTF-8"));
                        }
                        alternateAddressList.add(new String(uri, "UTF-8"));
                    } catch (UnsupportedEncodingException ignore) {
                    }
                }
            }

            public void connected() {
                changed();
            }

            @Override
            public void disconnected() {
                changed();
            }
        });
    }

    public List<String> getAlternateAddressList() {
        return new ArrayList(alternateAddressList);
    }

}
