package io.fabric8.insight.elasticsearch.impl;

import com.sonian.elasticsearch.zookeeper.discovery.ZooKeeperDiscoveryModule;
import io.fabric8.api.FabricService;
import io.fabric8.api.scr.ValidatingReference;
import org.apache.felix.scr.annotations.*;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class ElasticSearchNode implements Node {

    private String clusterName = "insight";

    private boolean dataEnabled = true;

    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    private org.elasticsearch.node.Node nodeDelegate;

    public void init() {
        FabricService fs = fabricService.get();

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("discovery.enabled", true)
                .put("discovery.type", ZooKeeperDiscoveryModule.class)
                .put("sonian.elasticsearch.zookeeper.client.host", fs.getZookeeperUrl())
                .put("sonian.elasticsearch.zookeeper.client.username", fs.getZooKeeperUser())
                .put("sonian.elasticsearch.zookeeper.client.username", fs.getZookeeperPassword())
                .put("sonian.elasticsearch.zookeeper.discovery.state_publishing.enabled", true)
                .build();

        nodeDelegate = nodeBuilder().data(dataEnabled).client(!dataEnabled).clusterName(clusterName).settings(settings).node();

        nodeDelegate.start();
    }

    public void destroy() {
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

    public void setFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    public void unsetFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setDataEnabled(boolean dataEnabled) {
        this.dataEnabled = dataEnabled;
    }
}
