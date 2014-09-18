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
