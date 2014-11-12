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
package io.fabric8.insight.elasticsearch.impl;

import org.apache.felix.scr.annotations.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@Component(name = "io.fabric8.elasticsearch", policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = false)
@Service(org.elasticsearch.node.Node.class)
public class ElasticsearchNode implements Node {

    private org.elasticsearch.node.Node nodeDelegate;

    @Activate
    protected void activate(final Map<String, Object> props) throws Exception {
        Map<String,String> stringProps = new HashMap<String,String>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            stringProps.put(entry.getKey(), entry.getValue().toString());
        }

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        String configFilePath = stringProps.get("config");
        if (configFilePath != null) {
            settings.loadFromUrl(new URL(configFilePath));
        }

        settings.put(stringProps).classLoader(getClass().getClassLoader());

        nodeDelegate = nodeBuilder().settings(settings).node();
    }

    @Deactivate
    protected void deactivate() {
        if (nodeDelegate != null && !nodeDelegate.isClosed()) {
            nodeDelegate.close();
        }
    }

    @Override
    public Settings settings() {
        return nodeDelegate.settings();
    }

    @Override
    public Client client() {
        return nodeDelegate.client();
    }

    @Override
    public Node start() {
        return nodeDelegate.start();
    }

    @Override
    public Node stop() {
        return nodeDelegate.stop();
    }

    @Override
    public void close() {
        nodeDelegate.close();
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
