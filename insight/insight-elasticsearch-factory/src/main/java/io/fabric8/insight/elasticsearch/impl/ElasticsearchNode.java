package io.fabric8.insight.elasticsearch.impl;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.Configurer;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@Component(name = "io.fabric8.elasticsearch", policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true)
@Service(org.elasticsearch.node.Node.class)
public class ElasticsearchNode implements Node {

    @Property(name = "cluster.name", value = "elasticsearch", label = "Cluster Name", description = "The name of the cluster this node should be a part of")
    private String clusterName;

    @Property(name = "node.data", boolValue = true, label = "Data Node?", description = "Is this a data node?")
    private boolean nodeData;

    @Property(name = "config", label = "Config File", description = "The URL to load the config file from (optional)")
    private String config;

    @Property(name = "rootNode", label = "Root Node", description = "The root node in ZooKeeper(optional)")
    private String rootNode;

    @Reference
    private Configurer configurer;

    private org.elasticsearch.node.Node nodeDelegate;

    @Activate
    protected void activate(final Map<String, Object> props) throws Exception {
        configurer.configure(props, this);

        Map<String,String> stringProps = new HashMap<String,String>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            stringProps.put(entry.getKey(), entry.getValue().toString());
        }

        Thread.currentThread().setContextClassLoader(Settings.class.getClassLoader());

        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        String configFilePath = stringProps.get("config");
        if (configFilePath != null) {
            settings.loadFromUrl(new URL(config));
        }

        settings.put(stringProps).classLoader(Settings.class.getClassLoader());

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
