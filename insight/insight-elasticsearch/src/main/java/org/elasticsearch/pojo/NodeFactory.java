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
package org.elasticsearch.pojo;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalNode;
import org.fusesource.insight.elasticsearch.ElasticRest;
import org.fusesource.insight.storage.StorageService;
import org.osgi.framework.BundleContext;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;

public class NodeFactory extends BaseManagedServiceFactory<ExtendedInternalNode> {

    private Map<String,String> settings;

    public NodeFactory(BundleContext context, Map<String, String> settings) {
        super(context, "ElasticSearch Node factory");
        this.settings = settings;
    }

    @Override
    protected ExtendedInternalNode doCreate(final Dictionary properties) {
        // If we want to use HDFS, we need to run under no user credentials so
        // that the default hdfs security mechanism will be used.
        return Subject.doAs(null, new PrivilegedAction<ExtendedInternalNode>() {
            @Override
            public ExtendedInternalNode run() {
                return doCreateInternal(properties);
            }
        });
    }

    protected ExtendedInternalNode doCreateInternal(Dictionary properties) {
        ImmutableSettings.Builder builder = ImmutableSettings.settingsBuilder();
        builder.put(settings);
        builder.classLoader(NodeFactory.class.getClassLoader());
        if (properties != null) {
            for (Enumeration e = properties.keys(); e.hasMoreElements();) {
                String key = e.nextElement().toString();
                Object oval = properties.get(key);
                String val = oval != null ? oval.toString() : null;
                builder.put(key, val);
            }
        }
        ExtendedInternalNode node = new ExtendedInternalNode(context, new InternalNode(builder.build(), false));
        try {
            node.start();
        } catch (RuntimeException t) {
            doDestroy(node);
            throw t;
        }
        return node;
    }

    @Override
    protected void doDestroy(ExtendedInternalNode node) {
        node.close();
    }

    @Override
    protected String[] getExposedClasses(ExtendedInternalNode node) {
        return new String[] { Node.class.getName(), ElasticRest.class.getName(), StorageService.class.getName() };
    }

}
