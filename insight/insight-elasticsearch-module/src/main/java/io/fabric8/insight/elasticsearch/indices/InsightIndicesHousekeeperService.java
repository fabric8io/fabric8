package io.fabric8.insight.elasticsearch.indices;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.hppc.cursors.ObjectObjectCursor;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.joda.time.Days;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InsightIndicesHousekeeperService extends AbstractLifecycleComponent<InsightIndicesHousekeeperService> {

    private final Settings settings;

    private final ThreadPool threadPool;

    private final Injector injector;

    private final String indicesPrefix;

    private final int daysOpened;

    private final int daysClosed;

    private final int daysStored;

    private final TimeValue interval;

    private ScheduledFuture<?> future;

    @Inject
    protected InsightIndicesHousekeeperService(Settings settings, ThreadPool threadPool, Injector injector) {
        super(settings);

        this.threadPool = threadPool;
        this.injector = injector;

        this.settings = settings.getByPrefix("insight.indices.management.");
        indicesPrefix = this.settings.get("prefix", "insight");
        daysOpened = this.settings.getAsInt("opened", 7);
        daysClosed = this.settings.getAsInt("closed", 14);
        daysStored = this.settings.getAsInt("stored", 0);

        interval = TimeValue.parseTimeValue(settings.get("interval"), TimeValue.timeValueHours(1));

        logger.info("Initialized {}", getClass().getSimpleName());
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        logger.info("Starting {}", getClass().getSimpleName());

        TimeValue interval = TimeValue.parseTimeValue(settings.get("initial"), TimeValue.timeValueHours(1));
        future = threadPool.schedule(interval, ThreadPool.Names.GENERIC, new Task());
    }

    @Override
    protected void doStop() throws ElasticsearchException {
        logger.info("Stopping {}", getClass().getSimpleName());

        if (future != null && !future.isCancelled()) {
            future.cancel(false);
        }
    }

    @Override
    protected void doClose() throws ElasticsearchException {
        if (future != null && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    class Task implements Runnable {

        private final Pattern pattern = Pattern.compile(indicesPrefix + "-([0-9]{4})\\.([0-9]{2})\\.([0-9]{2})");

        @Override
        public void run() {
            boolean reschedule = true;
            try {
                AdminClient adminClient = injector.getInstance(AdminClient.class);
                ClusterAdminClient clusterAdminClient = adminClient.cluster();

                ClusterStateResponse state = clusterAdminClient.state(clusterAdminClient.prepareState().request()).actionGet();

                if (!state.getState().nodes().getLocalNode().isMasterNode()) {
                    return;
                }

                Set<String> toClose = new HashSet<>();
                Set<String> toDelete = new HashSet<>();

                // Compute things to do
                LocalDate now = new LocalDate();
                for (ObjectObjectCursor<String, IndexMetaData> it : state.getState().metaData().indices()) {
                    String index = it.value.getIndex();
                    Matcher matcher = pattern.matcher(index);

                    if (matcher.find()) {
                        LocalDate date = new LocalDate(Integer.parseInt(matcher.group(1)),
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3)));

                        int daysOld = Days.daysBetween(date, now).getDays();
                        if (daysOld > 0 && daysOld > daysOpened) {
                            if (daysOld > daysOpened && daysOld <= daysClosed) {
                                logger.debug("Adding index to close: Index {} is {} day(s) old", index, daysOld);
                                toClose.add(index);
                            } else if (daysOld > daysClosed && daysOld > daysStored) {
                                logger.debug("Adding index to delete: Index {} is {} day(s) old", index, daysOld);
                                toDelete.add(index);
                            }
                        } else {
                            logger.debug("Ignoring index: Index {} is only {} day(s) old", index, daysOld);
                        }
                    } else {
                        logger.debug("Ignoring index: Name {} does not match the supported pattern ({})", index, pattern.pattern());
                    }

                    IndicesAdminClient indicesAdminClient = adminClient.indices();

                    if (!toClose.isEmpty()) {
                        logger.info("Closing indices: {}", toClose);
                        String[] indices = toClose.toArray(new String[toClose.size()]);
                        CloseIndexRequest req = indicesAdminClient.prepareClose(indices).request();
                        indicesAdminClient.close(req).actionGet();
                    }

                    if (!toDelete.isEmpty()) {
                        logger.info("Deleting indices: {}", toDelete);
                        String[] indices = toDelete.toArray(new String[toDelete.size()]);
                        DeleteIndexRequest req = indicesAdminClient.prepareDelete(indices).request();
                        indicesAdminClient.delete(req).actionGet();
                    }
                }


            } catch (ElasticsearchException e) {
                if (e.getCause() instanceof InterruptedException) {
                    reschedule = true;
                } else {
                    logger.error("Error performing indices management", e);
                }
            } finally {
                if (reschedule) {
                    future = threadPool.schedule(interval, ThreadPool.Names.GENERIC, this);
                }
            }
        }

    }
}
