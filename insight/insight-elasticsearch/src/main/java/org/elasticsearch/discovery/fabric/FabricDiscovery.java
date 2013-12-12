/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.discovery.fabric;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.ElasticSearchIllegalStateException;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ProcessedClusterStateUpdateTask;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.common.Base64;
import org.elasticsearch.common.UUID;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.InitialStateDiscoveryListener;
import org.elasticsearch.discovery.zen.DiscoveryNodesProvider;
import org.elasticsearch.discovery.zen.publish.PublishClusterStateAction;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import io.fabric8.groups.GroupListener;
import io.fabric8.groups.GroupFactory;
import io.fabric8.groups.NodeState;
import io.fabric8.groups.Group;
import io.fabric8.groups.internal.ZooKeeperGroupFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.elasticsearch.cluster.ClusterState.newClusterStateBuilder;
import static org.elasticsearch.cluster.node.DiscoveryNodes.newNodesBuilder;

public class FabricDiscovery extends AbstractLifecycleComponent<Discovery>
        implements Discovery,
                   DiscoveryNodesProvider,
                   ServiceTrackerCustomizer<CuratorFramework, CuratorFramework>,
                   PublishClusterStateAction.NewClusterStateListener,
        GroupListener<FabricDiscovery.ESNode> {

    private static final Logger LOG = LoggerFactory.getLogger(FabricDiscovery.class);

    protected final ClusterName clusterName;
    protected final ThreadPool threadPool;
    protected final TransportService transportService;
    protected final ClusterService clusterService;
    protected final NodeSettingsService nodeSettingsService;
    protected final DiscoveryNodeService discoveryNodeService;
    protected final BundleContext context;
    protected final ServiceTracker<CuratorFramework, CuratorFramework> tracker;

    private DiscoveryNode localNode;
    private final CopyOnWriteArrayList<InitialStateDiscoveryListener> initialStateListeners = new CopyOnWriteArrayList<InitialStateDiscoveryListener>();
    @Nullable private NodeService nodeService;
    private AllocationService allocationService;
    private volatile DiscoveryNodes latestDiscoNodes;
    private final PublishClusterStateAction publishClusterState;
    private volatile Group<ESNode> singleton;
    private final AtomicBoolean initialStateSent = new AtomicBoolean();

    @Inject
    public FabricDiscovery(Settings settings,
                           ClusterName clusterName,
                           ThreadPool threadPool,
                           TransportService transportService,
                           ClusterService clusterService,
                           NodeSettingsService nodeSettingsService,
                           DiscoveryNodeService discoveryNodeService) {
        super(settings);
        this.clusterName = clusterName;
        this.threadPool = threadPool;
        this.clusterService = clusterService;
        this.transportService = transportService;
        this.nodeSettingsService = nodeSettingsService;
        this.discoveryNodeService = discoveryNodeService;
        this.publishClusterState = new PublishClusterStateAction(settings, transportService, this, this);
        this.context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.tracker = new ServiceTracker<CuratorFramework, CuratorFramework>(context, CuratorFramework.class.getName(), this);
    }

    @Override
    protected void doStart() throws ElasticSearchException {
        Map<String, String> nodeAttributes = discoveryNodeService.buildAttributes();
        // note, we rely on the fact that its a new id each time we start, see FD and "kill -9" handling
        String nodeId = UUID.randomBase64UUID();
        String host = settings.get("discovery.publish.host");
        String port = settings.get("discovery.publish.port");
        if (host != null && port != null) {
            TransportAddress address = new InetSocketTransportAddress(host, Integer.parseInt(port));
            localNode = new DiscoveryNode(settings.get("name"), nodeId, address, nodeAttributes);
        } else {
            localNode = new DiscoveryNode(settings.get("name"), nodeId, transportService.boundAddress().publishAddress(), nodeAttributes);
        }
        tracker.open();
    }

    @Override
    protected void doStop() throws ElasticSearchException {
        tracker.close();
        initialStateSent.set(false);
    }

    @Override
    protected void doClose() throws ElasticSearchException {
        publishClusterState.close();
    }

    @Override
    public DiscoveryNode localNode() {
        return localNode;
    }

    @Override
    public void addListener(InitialStateDiscoveryListener listener) {
        initialStateListeners.add(listener);
    }

    @Override
    public void removeListener(InitialStateDiscoveryListener listener) {
        initialStateListeners.remove(listener);
    }

    @Override
    public String nodeDescription() {
        return clusterName.value() + "/" + localNode.id();
    }

    @Override
    public void setNodeService(@Nullable NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public void setAllocationService(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Override
    public void publish(ClusterState clusterState) {
        if (!singleton.isMaster()) {
            throw new ElasticSearchIllegalStateException("Shouldn't publish state when not master");
        }
        latestDiscoNodes = clusterState.nodes();
        publishClusterState.publish(clusterState);
    }

    @Override
    public DiscoveryNodes nodes() {
        DiscoveryNodes latestNodes = this.latestDiscoNodes;
        if (latestNodes != null) {
            return latestNodes;
        }
        // have not decided yet, just send the local node
        return newNodesBuilder().put(localNode).localNodeId(localNode.id()).build();
    }

    @Override
    public NodeService nodeService() {
        return this.nodeService;
    }


    @Override
    public CuratorFramework addingService(ServiceReference<CuratorFramework> reference) {
        CuratorFramework curator = context.getService(reference);
        try {
            GroupFactory factory = new ZooKeeperGroupFactory(curator);
            singleton = factory.createGroup("/fabric/registry/clusters/elasticsearch/" + clusterName.value(), ESNode.class);
            singleton.add(this);
            singleton.update(new ESNode(clusterName.value(), localNode, false));
            singleton.start();
        } catch (Exception e) {
            LOG.error("Error starting group", e);
        }
        return curator;
    }

    @Override
    public void modifiedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
    }

    @Override
    public void removedService(ServiceReference<CuratorFramework> reference, CuratorFramework service) {
        try {
            singleton.close();
        } catch (IOException e) {
            LOG.error("Error stopping group", e);
        }
        context.ungetService(reference);
    }

    @Override
    public void groupEvent(Group<ESNode> group, GroupEvent event) {
        // We need to set the TCCL because elasticsearch Settings will grab the wrong classloader if not
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(FabricDiscovery.class.getClassLoader());
            updateCluster();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private void updateCluster() {
        try {
            singleton.update(new ESNode(clusterName.value(), localNode, singleton.isMaster()));
        } catch (Exception e) {
            // Ignore if not joined
        }
        if (singleton.isMaster()) {
            clusterService.submitStateUpdateTask("fabric-discovery", new ProcessedClusterStateUpdateTask() {
                @Override
                public ClusterState execute(ClusterState currentState) {
                    // Rebuild state
                    ClusterState.Builder stateBuilder = newClusterStateBuilder().state(currentState);
                    // Rebuild nodes
                    DiscoveryNodes.Builder nodesBuilder = newNodesBuilder()
                            .localNodeId(localNode.id())
                            .masterNodeId(singleton.master().getNode().id())
                            .put(singleton.master().getNode());
                    for (ESNode node : singleton.slaves()) {
                        nodesBuilder.put(node.getNode());
                    }
                    latestDiscoNodes = nodesBuilder.build();
                    stateBuilder.nodes(latestDiscoNodes);
                    for (DiscoveryNode node : latestDiscoNodes) {
                        if (!currentState.nodes().nodeExists(node.id())) {
                            transportService.connectToNode(node);
                        }
                    }
                    // update the fact that we are the master...
                    if (!localNode().id().equals(currentState.nodes().masterNodeId())) {
                        ClusterBlocks clusterBlocks = ClusterBlocks.builder().blocks(currentState.blocks()).removeGlobalBlock(NO_MASTER_BLOCK).build();
                        stateBuilder.blocks(clusterBlocks);
                    }
                    return stateBuilder.build();
                }

                @Override
                public void clusterStateProcessed(ClusterState clusterState) {
                    sendInitialStateEventIfNeeded();
                }
            });
        } else if (singleton.master() != null) {
            DiscoveryNode masterNode = singleton.master().getNode();
            try {
                // first, make sure we can connect to the master
                transportService.connectToNode(masterNode);
            } catch (Exception e) {
                logger.warn("failed to connect to master [{}], retrying...", e, masterNode);
            }
        }

    }

    @Override
    public void onNewClusterState(final ClusterState newState) {
        if (singleton.isMaster()) {
            logger.warn("master should not receive new cluster state from [{}]", newState.nodes().masterNode());
        } else {
            if (newState.nodes().localNode() == null) {
                logger.warn("received a cluster state from [{}] and not part of the cluster, should not happen", newState.nodes().masterNode());
            } else {
                clusterService.submitStateUpdateTask("zen-disco-receive(from master [" + newState.nodes().masterNode() + "])", new ProcessedClusterStateUpdateTask() {
                    @Override
                    public ClusterState execute(ClusterState currentState) {
                        latestDiscoNodes = newState.nodes();

                        ClusterState.Builder builder = ClusterState.builder().state(newState);
                        // if the routing table did not change, use the original one
                        if (newState.routingTable().version() == currentState.routingTable().version()) {
                            builder.routingTable(currentState.routingTable());
                        }
                        // same for metadata
                        if (newState.metaData().version() == currentState.metaData().version()) {
                            builder.metaData(currentState.metaData());
                        } else {
                            // if its not the same version, only copy over new indices or ones that changed the version
                            MetaData.Builder metaDataBuilder = MetaData.builder().metaData(newState.metaData()).removeAllIndices();
                            for (IndexMetaData indexMetaData : newState.metaData()) {
                                IndexMetaData currentIndexMetaData = currentState.metaData().index(indexMetaData.index());
                                if (currentIndexMetaData == null || currentIndexMetaData.version() != indexMetaData.version()) {
                                    metaDataBuilder.put(indexMetaData, false);
                                } else {
                                    metaDataBuilder.put(currentIndexMetaData, false);
                                }
                            }
                            builder.metaData(metaDataBuilder);
                        }

                        return builder.build();
                    }

                    @Override
                    public void clusterStateProcessed(ClusterState clusterState) {
                        sendInitialStateEventIfNeeded();
                    }
                });
            }
        }
    }

    private void sendInitialStateEventIfNeeded() {
        if (initialStateSent.compareAndSet(false, true)) {
            for (InitialStateDiscoveryListener listener : initialStateListeners) {
                listener.initialStateProcessed();
            }
        }
    }

    @JsonSerialize(using = NodeSerializer.class)
    @JsonDeserialize(using = NodeDeserializer.class)
    static class ESNode extends NodeState {
        private final DiscoveryNode node;
        private final boolean master;

        ESNode(String id, DiscoveryNode node, boolean master) {
            super(id, node.getName());
            this.node = node;
            this.master = master;
        }

        public DiscoveryNode getNode() {
            return node;
        }

        public boolean isMaster() {
            return master;
        }
    }

    static class NodeSerializer extends JsonSerializer<ESNode> {
        @Override
        public void serialize(ESNode value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeStringField("id", value.getId());
            jgen.writeStringField("agent", System.getProperty("karaf.name"));
            if (value.isMaster()) {
                jgen.writeArrayFieldStart("services");
                jgen.writeString("elasticsearch");
                jgen.writeEndArray();
            }
            jgen.writeStringField("nodeName", value.getNode().name());
            jgen.writeStringField("nodeId", value.getNode().id());
            jgen.writeStringField("address", value.getNode().address().toString());
            jgen.writeStringField("version", value.getNode().version().toString());
            jgen.writeFieldName("attributes");
            jgen.writeStartObject();
            for (Map.Entry<String, String> entry : value.getNode().attributes().entrySet()) {
                jgen.writeStringField(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();
            jgen.writeStringField("binary", Base64.encodeObject(value.getNode()));
            jgen.writeEndObject();
        }
    }

    static class NodeDeserializer extends JsonDeserializer<ESNode> {
        @Override
        public ESNode deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            try {
                Map map = jp.readValueAs(Map.class);
                String id = map.get("id").toString();
                DiscoveryNode node = (DiscoveryNode) Base64.decodeToObject(map.get("binary").toString(), Base64.NO_OPTIONS, DiscoveryNode.class.getClassLoader());
                return new ESNode(id, node, false);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
    }

}
