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
package io.fabric8.gateway.fabric.support.http;

import io.fabric8.zookeeper.internal.SimplePathTemplate;
import io.fabric8.gateway.ServiceDetails;
import io.fabric8.gateway.handlers.http.HttpMappingRule;
import io.fabric8.gateway.handlers.http.MappedServices;
import io.fabric8.gateway.loadbalancer.LoadBalancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A set of HTTP mapping rules for applying add and remove service events to (typically from ZooKeeper but could be any discovery system).
 */
public class HttpMappingRuleBase implements HttpMappingRule {
    private static final transient Logger LOG = LoggerFactory.getLogger(HttpMappingRuleBase.class);

    private final SimplePathTemplate uriTemplate;
    /**
     * The version used if no "version" expression is used in the {@link #uriTemplate} and no
     * {@link #enabledVersion} is specified
     */
    private final String gatewayVersion;
    private final String enabledVersion;
    private final LoadBalancer loadBalancer;
    private final boolean reverseHeaders;

    private Map<String, MappedServices> mappingRules = new ConcurrentHashMap<String, MappedServices>();

    private Set<Runnable> changeListeners = new CopyOnWriteArraySet<Runnable>();

    public HttpMappingRuleBase(SimplePathTemplate uriTemplate, String gatewayVersion, String enabledVersion, LoadBalancer loadBalancer, boolean reverseHeaders) {
        this.uriTemplate = uriTemplate;
        this.gatewayVersion = gatewayVersion;
        this.enabledVersion = enabledVersion;
        this.loadBalancer = loadBalancer;
        this.reverseHeaders = reverseHeaders;
    }

    /**
     * Populates the parameters from the URL of the service so they can be reused in the URI template
     */
    public static void populateUrlParams(Map<String, String> params, String service) {
        try {
            URL url = new URL(service);
            params.put("contextPath", url.getPath());
            params.put("protocol", url.getProtocol());
            params.put("host", url.getHost());
            params.put("port", "" + url.getPort());

        } catch (MalformedURLException e) {
            LOG.warn("Invalid URL '" + service + "'. " + e);
        }
    }

    @Override
    public String toString() {
        return "HttpMappingRuleBase{" +
                "uriTemplate=" + uriTemplate +
                ", loadBalancer=" + loadBalancer +
                ", enabledVersion='" + enabledVersion + '\'' +
                ", reverseHeaders=" + reverseHeaders +
                ", gatewayVersion='" + gatewayVersion + '\'' +
                '}';
    }


    @Override
    public void appendMappedServices(Map<String, MappedServices> rules) {
        rules.putAll(mappingRules);
    }

    public String getGatewayVersion() {
        return gatewayVersion;
    }

    public SimplePathTemplate getUriTemplate() {
        return uriTemplate;
    }

    /**
     * Given a path being added or removed, update the services.
     *
     * @param remove        whether to remove (if true) or add (if false) this mapping
     * @param path          the path that this mapping is bound
     * @param services      the HTTP URLs of the services to map to
     * @param defaultParams the default parameters to use in the URI templates such as for version and container
     * @param serviceDetails
     */
    public void updateMappingRules(boolean remove, String path, List<String> services, Map<String, String> defaultParams, ServiceDetails serviceDetails) {
        SimplePathTemplate pathTemplate = getUriTemplate();
        if (pathTemplate != null) {
            boolean versionSpecificUri = pathTemplate.getParameterNames().contains("version");

            String versionId = defaultParams.get("version");
            if (!remove && versionId != null && !versionSpecificUri && gatewayVersion != null) {
                // lets ignore this mapping if the version does not match
                if (!gatewayVersion.equals(versionId)) {
                    remove = true;
                }
            }

            Map<String, String> params = new HashMap<String, String>();
            if (defaultParams != null) {
                params.putAll(defaultParams);
            }
            params.put("servicePath", path);

            for (String service : services) {
                populateUrlParams(params, service);
                String fullPath = pathTemplate.bindByNameNonStrict(params);
                if (remove) {
                    MappedServices rule = mappingRules.get(fullPath);
                    if (rule != null) {
                        List<String> serviceUrls = rule.getServiceUrls();
                        serviceUrls.remove(service);
                        if (serviceUrls.isEmpty()) {
                            mappingRules.remove(fullPath);
                        }
                    }
                } else {
                    MappedServices mappedServices = new MappedServices(service, serviceDetails, loadBalancer, reverseHeaders);
                    MappedServices oldRule = mappingRules.put(fullPath, mappedServices);
                    if (oldRule != null) {
                        mappedServices.getServiceUrls().addAll(oldRule.getServiceUrls());
                    }
                }
            }
        }
        fireMappingRulesChanged();
    }

    @Override
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    @Override
    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    protected void fireMappingRulesChanged() {
        for (Runnable changeListener : changeListeners) {
            changeListener.run();
        }
    }

}
