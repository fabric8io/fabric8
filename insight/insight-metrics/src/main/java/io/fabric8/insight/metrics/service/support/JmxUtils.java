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
package io.fabric8.insight.metrics.service.support;

import io.fabric8.insight.metrics.model.*;
import io.fabric8.insight.metrics.model.Query;

import javax.management.*;
import javax.management.openmbean.CompositeDataSupport;
import java.io.IOException;
import java.util.*;

public class JmxUtils {

    public static QueryResult execute(Server server, Query query, MBeanServerConnection mbs) throws JMException, IOException {
        // Iterate through queries
        Map<String, Result<?>> queryResults = new HashMap<String, Result<?>>();
        for (Request request : query.getRequests()) {
            queryResults.put(request.getName(), execute(server, request, mbs));
        }
        return new QueryResult(server, query, new Date(), queryResults);
    }

    private static Result execute(Server server, Request request, MBeanServerConnection mbs) throws JMException, IOException {
        if (request instanceof MBeanAttrs) {
            return execute(server, ((MBeanAttrs) request), mbs);
        } else if (request instanceof MBeanOpers) {
            return execute(server, ((MBeanOpers) request), mbs);
        } else {
            throw new IllegalArgumentException("Unsupported request " + request);
        }
    }

    public static MBeanOpersResult execute(Server server, MBeanOpers request, MBeanServerConnection mbs) throws JMException, IOException {
        List<MBeanOperResult> results = new ArrayList<MBeanOperResult>();
        // Get all mbeans
        Set<ObjectName> mbeans = mbs.queryNames(new ObjectName(request.getObj()), null);
        for (ObjectName mbean : mbeans) {
            // Invoke operation
            List<Object> args = request.getArgs();
            List<String> sig = request.getSig();
            Object value = mbs.invoke(mbean, request.getOper(),
                    args.toArray(new Object[args.size()]), sig.toArray(new String[sig.size()]));
            results.add(new MBeanOperResult(mbean, getJmxValue(value)));
        }
        return new MBeanOpersResult(request, results);
    }

    public static MBeanAttrsResult execute(Server server, MBeanAttrs request, MBeanServerConnection mbs) throws JMException, IOException {
        List<MBeanAttrResult> results = new ArrayList<MBeanAttrResult>();
        // Get all mbeans
        Set<ObjectName> mbeans = mbs.queryNames(new ObjectName(request.getObj()), null);
        for (ObjectName mbean : mbeans) {
            // Get list of attributes to query
            List<String> attrs = request.getAttrs();
            AttributeList al = mbs.getAttributes(mbean, attrs.toArray(new String[attrs.size()]));
            Map<String, Object> values = new HashMap<String, Object>();
            for (Attribute attribute : al.asList()) {
                values.put(attribute.getName(), getJmxValue(attribute.getValue()));
            }
            results.add(new MBeanAttrResult(mbean, values));
        }
        return new MBeanAttrsResult(request, results);
    }

    private static Object getJmxValue(Object value) {
        if (value instanceof CompositeDataSupport) {
            CompositeDataSupport cds = (CompositeDataSupport) value;
            Map<String, Object> map = new HashMap<String, Object>();
            for (String key : cds.getCompositeType().keySet()) {
                map.put(key, getJmxValue(cds.get(key)));
            }
            value = map;
        }
        return value;
    }
}
