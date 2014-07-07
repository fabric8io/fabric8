/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.elasticsearch.pojo;

import io.fabric8.common.util.JMXUtils;
import io.fabric8.insight.metrics.model.MetricsStorageService;
import io.fabric8.insight.metrics.model.QueryResult;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import io.fabric8.insight.elasticsearch.impl.ElasticRest;
import io.fabric8.insight.elasticsearch.impl.ElasticSearchServlet;
import io.fabric8.insight.elasticsearch.impl.ElasticStorageImpl;
import io.fabric8.insight.storage.StorageService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.IOException;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletException;

/**
 * Instead of registering 3 different services, we use a single wrapper which delegate to the
 * three services.  It helps management of service registration.
 */
public class ExtendedInternalNode implements Node, io.fabric8.insight.elasticsearch.ElasticRest, StorageService, MetricsStorageService {

    private final BundleContext bundleContext;
    private final ServiceTracker<HttpService, HttpService> httpServiceTracker;
    private final ServiceTracker<MBeanServer, MBeanServer> mbeanServerTracker;
    private final InternalNode node;
    private final ElasticRest rest;
    private final ElasticStorageImpl storage;
    private final ElasticSearchServlet servlet;

    private static ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("org.elasticsearch:service=restjmx");
        } catch (MalformedObjectNameException e) {
            // ignore
        }
    }

    public ExtendedInternalNode(final BundleContext bundleContext, InternalNode node) {
        this.bundleContext = bundleContext;
        this.httpServiceTracker = new ServiceTracker<HttpService, HttpService>(bundleContext, HttpService.class,
                new ServiceTrackerCustomizer<HttpService, HttpService>() {
                    @Override
                    public HttpService addingService(ServiceReference<HttpService> reference) {
                        HttpService service = ExtendedInternalNode.this.bundleContext.getService(reference);
                        try {
                            service.registerServlet("/elasticsearch", ExtendedInternalNode.this.servlet, null, null);
                        } catch (ServletException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } catch (NamespaceException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                        return service;
                    }
                    @Override
                    public void modifiedService(ServiceReference<HttpService> reference, HttpService service) {
                    }
                    @Override
                    public void removedService(ServiceReference<HttpService> reference, HttpService service) {
                        service.unregister("/elasticsearch");
                    }
                });
        this.mbeanServerTracker = new ServiceTracker<MBeanServer, MBeanServer>(bundleContext, MBeanServer.class,
                new ServiceTrackerCustomizer<MBeanServer, MBeanServer>() {
                    @Override
                    public MBeanServer addingService(ServiceReference<MBeanServer> reference) {
                        MBeanServer mBeanServer = bundleContext.getService(reference);
                        try {
                            JMXUtils.registerMBean(rest, mBeanServer, OBJECT_NAME);
                        } catch (Exception e) {
                            // Ignore
                            e.printStackTrace();
                        }
                        return mBeanServer;
                    }
                    @Override
                    public void modifiedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
                    }
                    @Override
                    public void removedService(ServiceReference<MBeanServer> reference, MBeanServer service) {
                        try {
                            JMXUtils.unregisterMBean(service, OBJECT_NAME);
                        } catch (Exception e) {
                            // Ignore
                            e.printStackTrace();
                        }
                    }
                });
        this.node = node;
        this.rest = new ElasticRest(node);
        this.storage = new ElasticStorageImpl(node);
        this.servlet = new ElasticSearchServlet(this);
    }

    @Override
    public Node start() {
        Node n = this.node.start();
        this.storage.init();
        this.httpServiceTracker.open();
        this.mbeanServerTracker.open();
        return n;
    }

    @Override
    public Node stop() {
        this.mbeanServerTracker.close();
        this.httpServiceTracker.close();
        this.storage.destroy();
        return this.node.stop();
    }

    @Override
    public void close() {
        stop();
        this.node.close();
    }

    @Override
    public Settings settings() {
        return this.node.settings();
    }

    @Override
    public Client client() {
        return this.node.client();
    }

    @Override
    public boolean isClosed() {
        return this.node.isClosed();
    }

    @Override
    public String get(String uri) throws IOException {
        return this.rest.get(uri);
    }

    @Override
    public String post(String uri, String content) throws IOException {
        return this.rest.post(uri, content);
    }

    @Override
    public String put(String uri, String content) throws IOException {
        return this.rest.put(uri, content);
    }

    @Override
    public String delete(String uri) throws IOException {
        return this.rest.delete(uri);
    }

    @Override
    public String head(String uri) throws IOException {
        return this.rest.head(uri);
    }

    @Override
    public void store(String type, long timestamp, String jsonData) {
        this.storage.store(type, timestamp, jsonData);
    }

    @Override
    public void store(String type, long timestamp, QueryResult queryResult) {
        this.storage.store(type, timestamp, queryResult);
    }
}
