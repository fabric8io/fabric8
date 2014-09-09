package io.fabric8.insight.elasticsearch.impl;

import io.fabric8.api.FabricService;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

@Component(configurationPid = "io.fabric8.insight.elasticsearch", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public class ElasticSearchNode implements Node {

    @Property(name = "cluster.name",
            label = "Cluster name", description = "Elasticsearch cluster name.")
    private String clusterName = "insight";

    @Property(name = "node.data", boolValue = true,
            label = "Data enabled?", description = "Is this an Elasticsearch data node? For production, you might want to create a separate cluster & set this to false so it acts simply as a client.")
    private boolean dataEnabled = true;

    @Reference(referenceInterface = FabricService.class)
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private org.elasticsearch.node.Node nodeDelegate;

    @Activate
    public void activate() {
        FabricService fs = fabricService.get();
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("discover.enabled", true)
                .put("discovery.type", "com.sonian.elasticsearch.zookeeper.discovery.ZooKeeper")
                .put("sonian.elasticsearch.zookeeper.client.host", fs.getZookeeperUrl())
                .put("sonian.elasticsearch.zookeeper.client.username", fs.getZooKeeperUser())
                .put("sonian.elasticsearch.zookeeper.client.username", fs.getZookeeperPassword())
                .put("sonian.elasticsearch.zookeeper.discovery.state_publishing.enabled", true)
                .build();

        nodeDelegate = nodeBuilder().data(dataEnabled).client(true).clusterName(clusterName).settings(settings).node();
    }

    @Deactivate
    public void deactivate() {
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

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
}
