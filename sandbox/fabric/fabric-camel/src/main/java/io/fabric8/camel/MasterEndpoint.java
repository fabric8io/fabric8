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
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents an endpoint which only becomes active when it obtains the master lock
 */
public class MasterEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(MasterEndpoint.class);

    private final MasterComponent component;
    private final String singletonId;
    private final String child;


    public MasterEndpoint(String uri, MasterComponent component, String singletonId, String child) {
        super(uri, component);
        this.component = component;
        this.singletonId = singletonId;
        this.child = child;
    }

    public String getSingletonId() {
        return singletonId;
    }

    @Override
    public Producer createProducer() throws Exception {
        return getChildEndpoint().createProducer();
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new MasterConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public boolean isLenientProperties() {
        // to allow properties to be propagated to the child endpoint
        return true;
    }

    // Properties
    //-------------------------------------------------------------------------
    public MasterComponent getComponent() {
        return component;
    }

    public String getChild() {
        return child;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    Endpoint getChildEndpoint() {
        return getCamelContext().getEndpoint(child);
    }
}
