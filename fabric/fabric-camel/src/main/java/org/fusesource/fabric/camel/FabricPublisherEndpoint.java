/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.Group;

/**
 * Creates an endpoint which uses FABRIC to map a logical name to physical endpoint names
 */
public class FabricPublisherEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(FabricPublisherEndpoint.class);

    private final FabricComponent component;
    private final Group group;
    private final String child;

    public FabricPublisherEndpoint(String uri, FabricComponent component, Group group, String child) {
        super(uri, component);
        this.component = component;
        this.group = group;
        this.child = child;
    }

    public Producer createProducer() throws Exception {
        return getCamelContext().getEndpoint(child).createProducer();
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return getCamelContext().getEndpoint(child).createConsumer(processor);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void start() throws Exception {
        super.start();
        group.join(child.getBytes("UTF-8"));
    }

    @Override
    public void stop() throws Exception {
        group.close();
        super.stop();
    }

    // Properties
    //-------------------------------------------------------------------------
    public FabricComponent getComponent() {
        return component;
    }

    public String getChild() {
        return child;
    }
}
