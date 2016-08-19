/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.karaf.checks.internal;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.fabric8.karaf.checks.Check;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

public class CamelState extends AbstractChecker {

    private ServiceTracker<MBeanServer, MBeanServer> mbeanServer;

    public CamelState() {
        this.mbeanServer = new ServiceTracker<>(bundleContext, MBeanServer.class, null);
        this.mbeanServer.open();
    }

    @Override
    protected List<Check> doCheck() {
        MBeanServer server = this.mbeanServer.getService();
        if (server != null) {
            try {
                List<Check> checks = new ArrayList<>();
                Set<ObjectName> contexts = server.queryNames(new ObjectName("org.apache.camel:type=context,*"), null);
                for (ObjectName ctxName : contexts) {
                    String state = server.getAttribute(ctxName, "State").toString();
                    if (!"Started".equals(state)) {
                        String name = ctxName.getKeyProperty("name");
                        checks.add(new Check("camel-state", "Camel context " + name + " is in state " + state));
                    }
                }
                return checks;
            } catch (Exception e) {
                return Collections.singletonList(new Check("camel-state", "Unable to check camel contexts: " + e.toString()));
            }
        }
        return Collections.emptyList();
    }
}
