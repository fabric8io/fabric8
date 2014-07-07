package io.fabric8.insight.elasticsearch.indices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequest;
import org.elasticsearch.action.admin.cluster.snapshots.create.CreateSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.delete.DeleteSnapshotRequest;
import org.elasticsearch.action.admin.cluster.snapshots.get.GetSnapshotsResponse;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.optimize.OptimizeRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.RepositoriesMetaData;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.joda.time.Days;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.snapshots.SnapshotInfo;
import org.elasticsearch.threadpool.ThreadPool;

public class IndicesManagementService extends AbstractLifecycleComponent<IndicesManagementService> {

    protected final ThreadPool threadPool;
    protected final Injector injector;

    protected ScheduledFuture<?> future;

    @Inject
    public IndicesManagementService(Settings settings,
                                    ThreadPool threadPool,
                                    Injector injector) {
        super(settings);
        this.threadPool = threadPool;
        this.injector = injector;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        TimeValue interval = TimeValue.parseTimeValue(settings.get("initial"), TimeValue.timeValueHours(1));
        future = threadPool.schedule(interval, ThreadPool.Names.GENERIC, new Task());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        if (future != null) {
            future.cancel(false);
        }
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        if (future != null) {
            future.cancel(true);
        }
    }

    class Task implements Runnable {
        @Override
        public void run() {
            boolean reschedule = true;
            try {
                Settings settings = IndicesManagementService.this.settings.getByPrefix("insight.indices.management.");
                Set<String> repositories = settings.getByPrefix("repository.").getAsStructuredMap().keySet();
                int opened = settings.getAsInt("opened", 7);
                int closed = settings.getAsInt("closed", 14);
                int stored = settings.getAsInt("stored", 0);

                Set<String> toOptimize = new HashSet<>();
                Set<String> toStore = new HashSet<>();
                Set<String> toClose = new HashSet<>();
                Set<String> toDelete = new HashSet<>();

                AdminClient adminClient = injector.getInstance(AdminClient.class);
                ClusterAdminClient clusterAdminClient = adminClient.cluster();
                IndicesAdminClient indicesAdminClient = adminClient.indices();

                ClusterStateResponse state = clusterAdminClient.state(clusterAdminClient.prepareState().request()).actionGet();

                // Only execute on master node
                String masterNodeId = state.getState().nodes().masterNodeId();
                String localNodeId = state.getState().nodes().getLocalNodeId();
                if (localNodeId == null || !localNodeId.equals(masterNodeId)) {
                    return;
                }

                // Compute things to do
                LocalDate now = new LocalDate();
                Pattern pattern = Pattern.compile(".*-([0-9]{4})\\.([0-9]{2})\\.([0-9]{2})");
                for (ObjectObjectCursor<String, IndexMetaData> it : state.getState().metaData().indices()) {
                    String index = it.value.getIndex();
                    Matcher matcher = pattern.matcher(index);
                    if (matcher.find()) {
                        LocalDate date = new LocalDate(Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3)));

                        int days = Days.daysBetween(date, now).getDays();
                        if (days > 0) {
                            toOptimize.add(index);
                            if (stored < 0 || days <= stored) {
                                toStore.add(index);
                            }
                            if (opened >= 0 && days > opened) {
                                if (closed < 0 || days <= closed) {
                                    toClose.add(index);
                                } else {
                                    toDelete.add(index);
                                }
                            }
                        }
                    } else {
                        logger.warn("Index name {} does not match the supported pattern");
                    }
                }

                // Optimize
                if (!toOptimize.isEmpty()) {
                    logger.info("Optimizing indices " + toOptimize);
                    String[] indices = toOptimize.toArray(new String[toOptimize.size()]);
                    OptimizeRequest request = indicesAdminClient
                            .prepareOptimize(indices)
                            .setMaxNumSegments(1)
                            .request();
                    indicesAdminClient.optimize(request).actionGet();
                }
                // Create snapshots
                if (!toStore.isEmpty()) {
                    if (repositories.isEmpty()) {
                        logger.error("No repository defined for storing indices");
                        return;
                    }
                    RepositoriesMetaData repositoriesMetaData = (RepositoriesMetaData) state.getState().getMetaData().getCustoms().get("repositories");
                    logger.info("Storing indices " + toStore);
                    for (String repository : repositories) {
                        RepositoryMetaData repositoryMetaData = null;
                        for (RepositoryMetaData rmd : repositoriesMetaData.repositories()) {
                            if (repository.equals(rmd.name())) {
                                repositoryMetaData = rmd;
                                break;
                            }
                        }
                        Settings repoSettings = settings.getByPrefix("repository." + repository + ".");
                        String type = repoSettings.get("type");
                        Settings set = repoSettings.getByPrefix("settings.");
                        if (type != null && set != null) {
                            if (repositoryMetaData == null ||
                                    !type.equals(repositoryMetaData.type()) ||
                                    !set.getAsMap().equals(repositoryMetaData.settings().getAsMap())) {
                                logger.info("Updating repository definition for " + repository);
                                PutRepositoryRequest req = clusterAdminClient.preparePutRepository(repository)
                                        .setType(type)
                                        .setSettings(repoSettings.getByPrefix("settings."))
                                        .request();
                                clusterAdminClient.putRepository(req).actionGet();
                            }
                        } else {
                            if (repositoryMetaData == null) {
                                logger.warn("Repository " + repository + " is not defined");
                                continue;
                            }
                        }
                        GetSnapshotsResponse snapshots = clusterAdminClient.getSnapshots(clusterAdminClient.prepareGetSnapshots(repository).request()).actionGet();
                        Map<String, SnapshotInfo> infos = new HashMap<>();
                        for (SnapshotInfo info : snapshots.getSnapshots()) {
                            infos.put(info.name(), info);
                        }
                        for (String index : toStore) {
                            SnapshotInfo info = infos.get(index);
                            if (info != null) {
                                // The snapshot is not finished
                                if (info.endTime() == 0L) {
                                    continue;
                                }
                                if (info.failedShards() > 0) {
                                    DeleteSnapshotRequest req = clusterAdminClient.prepareDeleteSnapshot(repository, index).request();
                                    clusterAdminClient.deleteSnapshot(req).actionGet();
                                    info = null;
                                }
                            }
                            if (info == null) {
                                CreateSnapshotRequest req = clusterAdminClient.prepareCreateSnapshot(repository, index)
                                        .setIncludeGlobalState(false)
                                        .setIndices(index)
                                        .setWaitForCompletion(true)
                                        .request();
                                clusterAdminClient.createSnapshot(req).actionGet();
                            }
                        }
                    }
                }
                // Closing indices
                if (!toClose.isEmpty()) {
                    logger.info("Closing indices " + toClose);
                    String[] indices = toClose.toArray(new String[toClose.size()]);
                    CloseIndexRequest req = indicesAdminClient.prepareClose(indices).request();
                    indicesAdminClient.close(req).actionGet();
                }
                // Deleting indices
                if (!toDelete.isEmpty()) {
                    logger.info("Deleting indices " + toDelete);
                    String[] indices = toDelete.toArray(new String[toDelete.size()]);
                    DeleteIndexRequest req = indicesAdminClient.prepareDelete(indices).request();
                    indicesAdminClient.delete(req).actionGet();
                }

                // TODO: delete old backups
            } catch (ElasticsearchException e) {
                if (e.getCause() instanceof InterruptedException)  {
                    reschedule = true;
                } else {
                    logger.error("Error performing indices management", e);
                }
            } finally {
                if (reschedule) {
                    TimeValue interval = TimeValue.parseTimeValue(settings.get("interval"), TimeValue.timeValueHours(8));
                    future = threadPool.schedule(interval, ThreadPool.Names.GENERIC, this);
                }
            }
        }
    }
}
