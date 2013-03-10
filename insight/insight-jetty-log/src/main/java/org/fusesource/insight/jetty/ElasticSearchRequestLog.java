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

package org.fusesource.insight.jetty;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.elasticsearch.action.index.IndexRequest;
import org.fusesource.insight.elasticsearch.ElasticSender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.fusesource.insight.jetty.InsightUtils.formatDate;
import static org.fusesource.insight.jetty.InsightUtils.getIndex;

public class ElasticSearchRequestLog extends AbstractLifeCycle implements RequestLog, ManagedService {

    private static final Logger LOG = Log.getLogger(ElasticSearchRequestLog.class);

    private final BundleContext bundleContext;
    private final ServiceTracker<ElasticSender, ElasticSender> sender;
    private final String host = System.getProperty("karaf.name");
    private ServiceRegistration<ManagedService> registration;

    private Dictionary<String, ?> properties;
    private boolean enabled = true;
    private String index = "insight";
    private String type = "jetty";
    private PathMap ignorePathMap;

    public ElasticSearchRequestLog() {
        this.bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        sender = new ServiceTracker<ElasticSender, ElasticSender>(bundleContext, ElasticSender.class, null);
    }

    @Override
    protected void doStart() throws Exception {
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_PID, "org.fusesource.insight.jetty");
        this.registration = this.bundleContext.registerService(ManagedService.class, this, props);
        this.sender.open();
    }

    @Override
    protected void doStop() throws Exception {
        sender.close();
        this.registration.unregister();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.properties = properties;
        this.enabled = getBoolean("enabled", true);
        this.index = getString("index", "insight");
        this.type = getString("type", "jetty");
        String ignorePaths = getString("ignorePaths", "");
        if (ignorePaths != null && ignorePaths.length() > 0) {
            ignorePathMap = new PathMap();
            for (String s : ignorePaths.split(",")) {
                ignorePathMap.put(s, s);
            }
        }
        else {
            ignorePathMap = null;
        }

    }

    private String getString(String name, String def) {
        Object v = this.properties.get(name);
        if (v == null) {
            return def;
        } else {
            return v.toString();
        }
    }

    private boolean getBoolean(String name, boolean def) {
        Object v = this.properties.get(name);
        if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v == null) {
            return def;
        } else {
            return Boolean.parseBoolean(v.toString());
        }
    }

    @Override
    public void log(Request request, Response response) {
        try {
            if (!enabled) {
                return;
            }

            ElasticSender s = sender.getService();
            if (s == null) {
                return;
            }

            if (ignorePathMap != null && ignorePathMap.getMatch(request.getRequestURI()) != null)
                return;

            String output = "{ " +
                    "\"host\": \"" + host + "\", " +
                    "\"timestamp\": \"" + formatDate(request.getTimeStamp()) + "\", " +
                    "\"remote\": \"" + request.getRemoteAddr() + "\", " +
                    "\"user\": \"" + (request.getAuthentication() instanceof Authentication.User ? ((Authentication.User)request.getAuthentication()).getUserIdentity().getUserPrincipal().getName() : "") + "\", " +
                    "\"method\": \"" + request.getMethod() + "\", " +
                    "\"uri\": \"" + request.getUri().toString() + "\", " +
                    "\"protocol\": \"" + request.getProtocol() + "\", " +
                    "\"status\": \"" + response.getStatus() + "\", " +
                    "\"responseLength\": \"" + response.getContentCount() + "\" " +
                    " }";

            IndexRequest ir = new IndexRequest()
                    .index(getIndex(index, request.getTimeStamp()))
                    .type(type)
                    .source(output)
                    .create(true);
            s.push(ir);
        }
        catch (Exception e)
        {
            LOG.warn(e);
        }
    }

}
