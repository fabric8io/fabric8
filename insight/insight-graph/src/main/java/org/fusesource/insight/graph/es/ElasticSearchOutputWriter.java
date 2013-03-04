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

package org.fusesource.insight.graph.es;

import com.googlecode.jmxtrans.OutputWriter;
import com.googlecode.jmxtrans.model.Query;
import com.googlecode.jmxtrans.model.Result;
import com.googlecode.jmxtrans.util.BaseOutputWriter;
import com.googlecode.jmxtrans.util.JmxUtils;
import com.googlecode.jmxtrans.util.LifecycleException;
import com.googlecode.jmxtrans.util.ValidationException;
import org.elasticsearch.action.index.IndexRequest;
import org.fusesource.insight.elasticsearch.ElasticSender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


public class ElasticSearchOutputWriter extends BaseOutputWriter implements OutputWriter {

    private BundleContext bundleContext;
    private ServiceTracker<ElasticSender, ElasticSender> tracker;
    private String index;
    private String type;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private SimpleDateFormat indexFormat = new SimpleDateFormat("yyyy.MM.dd");

    public ElasticSearchOutputWriter() {
        this.bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        this.tracker = new ServiceTracker<ElasticSender, ElasticSender>(bundleContext, ElasticSender.class, null);
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
                        writer.append(", \"value\" : ").append(toString(values.getValue()));
                        writer.append(" }");

                        IndexRequest request = new IndexRequest()
                                .index(getIndex(result.getEpoch()))
                                .type(type)
                                .source(writer.toString())
                                .create(true);

                        ElasticSender sender = tracker.getService();
                        if (sender != null) {
                            sender.push(request);
                        }
                    }
                }
            }
        }
    }

    private String toString(Object value) {
        double d;
        if (value instanceof Number) {
            d = ((Number) value).doubleValue();
        } else {
            d = Double.parseDouble(value.toString());
        }
        return Double.toString(d);
    }

    private String getIndex(long timestamp) {
        return this.index + "-"+ indexFormat.format(new Date(timestamp));
    }

    private String formatDate(long timestamp) {
        return simpleDateFormat.format(new Date(timestamp));
    }

    public String getStringSetting(String key, String defaultVal) {
        final Object value = this.getSettings().get(key);
        return value != null ? value.toString() : defaultVal;
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. A backslash will be inserted within </, producing <\/,
     * allowing JSON text to be delivered in HTML. In JSON text, a string
     * cannot contain a control character or an unescaped quote or backslash.
     * @param string A String
     * @return  A String correctly formatted for insertion in a JSON text.
     */
    public static void quote(String string, StringBuilder w) {
        if (string == null || string.length() == 0) {
            w.append("\"\"");
            return;
        }

        char         b;
        char         c = 0;
        String       hhhh;
        int          i;
        int          len = string.length();

        w.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.append('\\');
                    w.append(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.append('\\');
                    }
                    w.append(c);
                    break;
                case '\b':
                    w.append("\\b");
                    break;
                case '\t':
                    w.append("\\t");
                    break;
                case '\n':
                    w.append("\\n");
                    break;
                case '\f':
                    w.append("\\f");
                    break;
                case '\r':
                    w.append("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0') ||
                            (c >= '\u2000' && c < '\u2100')) {
                        hhhh = "000" + Integer.toHexString(c);
                        w.append("\\u" + hhhh.substring(hhhh.length() - 4));
                    } else {
                        w.append(c);
                    }
            }
        }
        w.append('"');
    }
}
