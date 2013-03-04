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

package org.fusesource.insight.log.elasticsearch;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.fusesource.insight.elasticsearch.ElasticSender;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class SenderProxy implements ElasticSender {

    private BundleContext context;
    private ServiceTracker<ElasticSender, ElasticSender> tracker;

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void init() {
        this.tracker = new ServiceTracker<ElasticSender, ElasticSender>(context, ElasticSender.class, null);
        this.tracker.open();
    }

    public void destroy() {
        this.tracker.close();
    }

    @Override
    public void push(IndexRequest request) {
        ElasticSender sender = this.tracker.getService();
        if (sender != null) {
            sender.push(request);
        }
    }

    @Override
    public void push(DeleteRequest request) {
        ElasticSender sender = this.tracker.getService();
        if (sender != null) {
            sender.push(request);
        }
    }
}
