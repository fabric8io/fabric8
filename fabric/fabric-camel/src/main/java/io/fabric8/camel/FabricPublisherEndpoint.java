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
package io.fabric8.camel;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.camel.util.URISupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import io.fabric8.groups.Group;

import java.net.URI;
import java.util.Map;

/**
 * Creates an endpoint which uses FABRIC to map a logical name to physical endpoint names
 */
public class FabricPublisherEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(FabricPublisherEndpoint.class);

    private final FabricComponent component;
    private final String singletonId;
    private final String child;
    private final String consumer;
    private final Group<CamelNodeState> group;
    private String joined;

    public FabricPublisherEndpoint(String uri, FabricComponent component, String singletonId, String child) throws Exception {
        super(uri, component);
        this.component = component;
        this.singletonId = singletonId;

        String path = child;
        int idx = path.indexOf('?');
        if (idx > -1) {
            path = path.substring(0, idx);
        }
        Map<String, Object> params = URISupport.parseParameters(new URI(child));
        String consumer = params != null ? (String) params.remove("consumer") : null;
        if (consumer != null) {
            Map<String, Object> properties = IntrospectionSupport.extractProperties(params, "consumer.");
            if (properties != null && properties.size() > 0) {
                consumer = consumer + "?" + URISupport.createQueryString(properties);
                for (String k : properties.keySet()) {
                    params.remove(k);
                }
            }
            child = path;
            if (params.size() > 0) {
                child = child + "?" + URISupport.createQueryString(params);
            }
        } else {
            consumer = child;
        }
        LOG.info("Child: " + child);
        LOG.info("Consumer: " + consumer);
        this.child = child;
        this.consumer = consumer;

        path = getComponent().getFabricPath(singletonId);
        group = getComponent().createGroup(path);
        CamelNodeState state = new CamelNodeState(singletonId);
        state.consumer = consumer;
        group.update(state);
    }

    public Producer createProducer() throws Exception {
        return getCamelContext().getEndpoint(child).createProducer();
    }

    @Override
    public boolean isLenientProperties() {
        // to allow properties to be propagated to the child endpoint
        return true;
    }
    
    public Consumer createConsumer(Processor processor) throws Exception {
        return getCamelContext().getEndpoint(child).createConsumer(processor);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void doStart() throws Exception {
        super.doStart();
        group.start();
    }

    @Override
    public void doStop() throws Exception {
        group.close();
        super.doStop();
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
