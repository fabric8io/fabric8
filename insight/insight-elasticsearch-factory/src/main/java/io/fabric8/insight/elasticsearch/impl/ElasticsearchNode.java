package io.fabric8.insight.elasticsearch.impl;

import io.fabric8.api.scr.Configurer;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import java.net.URL;
import java.util.Map;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@Component(name = "io.fabric8.elasticsearch", policy = ConfigurationPolicy.REQUIRE, configurationFactory = true, metatype = true)
@Service(org.elasticsearch.node.Node.class)
public class ElasticsearchNode implements Node {

    @Property(name = "cluster.name", value="elasticsearch", label = "Cluster Name", description = "The name of the cluster this node should be a part of")
    private String clusterName;

    @Property(name = "node.data", boolValue = true, label = "Data Node?", description = "Is this a data node?")
    private boolean nodeData;

    @Property(name = "config", label = "Config File", description = "The URL to load the config file from (optional)")
    private String config;

    @Reference
    private Configurer configurer;

    private org.elasticsearch.node.Node nodeDelegate;

    @Activate
    protected void activate(final Map<String, ?> props) throws Exception {
        configurer.configure(props, this);

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder();

        if (config != null) {
            settings.loadFromUrl(new URL(config));
        }

        nodeDelegate = nodeBuilder().data(nodeData).client(!nodeData).clusterName(clusterName).settings(settings).node();
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
