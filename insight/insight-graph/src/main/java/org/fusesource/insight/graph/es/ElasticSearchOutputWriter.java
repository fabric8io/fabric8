package org.fusesource.insight.graph.es;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.node.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.fusesource.insight.graph.es.ElasticSender.quote;

public class ElasticSearchOutputWriter extends BaseOutputWriter implements OutputWriter, ServiceTrackerCustomizer<Node, Node> {

    // Making an assumption here that we will log less than 1,000,000 events/sec,  next this JVM
    // restarts, the next sequence number should be < any previously generated sequence numbers.
    static final private AtomicLong SEQUENCE_COUNTER = new AtomicLong(System.currentTimeMillis()*1000);

    private BundleContext bundleContext;
    private ServiceTracker<Node, Node> tracker;
    private String index;
    private String type;
    private ElasticSender sender;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public ElasticSearchOutputWriter() {
        this.sender = new ElasticSender();
        this.bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.tracker = new ServiceTracker<Node, Node>(bundleContext, Node.class, this);
    }

    @Override
    public void start() throws LifecycleException {
        index = getStringSetting("index", "insight");
        type = getStringSetting("type", "sta");
        tracker.open();
    }

    @Override
    public void stop() throws LifecycleException {
        tracker.close();
    }

    @Override
    public Node addingService(ServiceReference<Node> nodeServiceReference) {
        Node node = bundleContext.getService(nodeServiceReference);

        sender.setNode(node);

        CreateIndexRequest request = new CreateIndexRequest(index);

        HashMap<String, Object> properties = new HashMap<String, Object>();

        HashMap<String, Object> seq = new HashMap<String, Object>();
        seq.put("type", "string");
        seq.put("index", "not_analyzed");
        properties.put("seq", seq);

        HashMap<String, Object> value = new HashMap<String, Object>();
        value.put("type", "double");
        properties.put("value", value);

        HashMap<String, Object> options = new HashMap<String, Object>();
        options.put("properties", properties);
        request.mapping(type, options);

        sender.createIndexIfNeeded(request);

        sender.init();

        return node;
    }

    @Override
    public void modifiedService(ServiceReference<Node> nodeServiceReference, Node node) {
    }

    @Override
    public void removedService(ServiceReference<Node> nodeServiceReference, Node node) {
        sender.destroy();
        bundleContext.ungetService(nodeServiceReference);
    }

    @Override
    public void validateSetup(Query query) throws ValidationException {
    }

    @Override
    public void doWrite(Query query) throws Exception {
        for (Result result : query.getResults()) {
            Map<String, Object> resultValues = result.getValues();
            if (resultValues != null) {
                for (Map.Entry<String, Object> values : resultValues.entrySet()) {
                    if (JmxUtils.isNumeric(values.getValue())) {
                        StringBuilder writer = new StringBuilder();
                        writer.append("{ \"host\": ");
                        quote(query.getServer().getAlias(), writer);
                        writer.append(", \"seq\" : " + SEQUENCE_COUNTER.incrementAndGet());
                        writer.append(", \"timestamp\" : ");
                        quote(formatDate(result.getEpoch()), writer);
                        writer.append(", \"object\" : ");
                        quote(query.getObj(), writer);
                        writer.append(", \"attribute\" : ");
                        quote(result.getAttributeName(), writer);
                        writer.append(", \"class\" : ");
                        quote(result.getClassName(), writer);
                        writer.append(", \"alias\" : ");
                        quote(result.getClassNameAlias(), writer);
                        writer.append(", \"type\" : ");
                        quote(result.getTypeName(), writer);
                        writer.append(", \"key\" : ");
                        quote(values.getKey(), writer);
                        writer.append(", \"value\" : " + values.getValue().toString());
                        writer.append(" }");
                        IndexRequest request = new IndexRequest()
                                .index(index)
                                .type(type)
                                .source(writer.toString())
                                .create(true);
                        sender.put(request);
                    }
                }
            }
        }
    }

    private String formatDate(long timestamp) {
        return simpleDateFormat.format(new Date(timestamp));
    }

    public String getStringSetting(String key, String defaultVal) {
        final Object value = this.getSettings().get(key);
        return value != null ? value.toString() : defaultVal;
    }

}
