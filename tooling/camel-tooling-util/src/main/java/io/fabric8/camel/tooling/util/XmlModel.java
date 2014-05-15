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
package io.fabric8.camel.tooling.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Node;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.camel.spring.CamelContextFactoryBean;
import org.apache.camel.spring.CamelEndpointFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static io.fabric8.camel.tooling.util.CamelNamespaces.*;

public class XmlModel {

    public static Logger LOG = LoggerFactory.getLogger(XmlModel.class);

    private CamelContextFactoryBean contextElement;
    private Document doc;
    private Map<String, String> beans;
    private Node node;
    private String ns;
    private boolean justRoutes;
    private boolean routesContext;

    public XmlModel(CamelContextFactoryBean contextElement, Document doc, Map<String, String> beans, Node node, String ns, boolean justRoutes, boolean routesContext) {
        this.contextElement = contextElement;
        this.doc = doc;
        this.beans = beans;
        this.node = node;
        this.ns = ns;
        this.justRoutes = justRoutes;
        this.routesContext = routesContext;
    }

    public CamelContextFactoryBean getContextElement() {
        return contextElement;
    }

    public Document getDoc() {
        return doc;
    }

    public Map<String, String> getBeans() {
        return beans;
    }

    public Node getNode() {
        return node;
    }

    public String getNs() {
        return ns;
    }

    public boolean isJustRoutes() {
        return justRoutes;
    }

    public boolean isRoutesContext() {
        return routesContext;
    }

    /**
     * Returns the root element to be marshalled as XML
     *
     * @return
     */
    public Object marshalRootElement() {
        if (justRoutes) {
            RoutesDefinition routes = new RoutesDefinition();
            routes.setRoutes(contextElement.getRoutes());
            return routes;
        } else {
            return contextElement;
        }
    }

    /**
     * Creates a new model using the given context
     *
     * @param newContext
     */
    public void update(CamelContextFactoryBean newContext) {
        this.contextElement = newContext;
    }

    public List<RouteDefinition> getRouteDefinitionList() {
        return contextElement.getRoutes();
    }

    public boolean hasMissingId() {
        for (RouteDefinition rd : getRouteDefinitionList()) {
            if (rd.getId() == null) {
                return true;
            }
        }
        return false;
    }

    public CamelContext createContext(Collection<RouteDefinition> routes) throws Exception {
        ModelCamelContext context = new DefaultCamelContext();
        context.addRouteDefinitions(routes);
        return context;
    }

    public CamelContext camelContext() throws Exception {
        return createContext(contextElement.getRoutes());
    }

    /**
     * Returns the endpoint URIs used in the context
     *
     * @return
     */
    public Set<String> endpointUris() {
        try {
            // we must use reflection for now until Camel supports the getEndpoints() method
            // https://issues.apache.org/jira/browse/CAMEL-3644
            // ...
            // the above is no longer valid since Camel 2.7.0
            List<CamelEndpointFactoryBean> endpoints = contextElement.getEndpoints();
            List<String> uris = new LinkedList<String>();
            if (endpoints != null) {
                for (CamelEndpointFactoryBean cefb : endpoints) {
                    uris.add(cefb.getUri());
                }
            }

            // lets detect any drools endpoints...
            List<Node> sessions = nodesByNamespace(doc, droolsNamespace.getURI(), "ksession");
            if (sessions != null) {
                for (Node session: sessions) {
                    if (session instanceof Element) {
                        Element e = (Element) session;
                        String node = e.getAttributeValue("node");
                        String sid = e.getAttributeValue("id");
                        if (node != null && node.length() > 0 && sid != null && sid.length() > 0) {
                            String du = "drools:" + node + "/" + sid;
                            boolean exists = false;
                            for (String uri : uris) {
                                if (uri.startsWith(du)) {
                                    exists = true;
                                }
                            }
                            if (!exists) {
                                uris.add(du);
                            }
                        }
                    }
                }
            }
            return new TreeSet<String>(uris);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return new HashSet<String>();
        }
    }

    public Set<String> endpointUriSet() {
        return endpointUris();
    }

    /**
     * Returns a Java API for accessing the bean map
     *
     * @return
     */
    public Map<String, String> beanMap() {
        return beans;
    }

    public ValidationHandler validate() throws IOException, SAXException {
        ValidationHandler v = new ValidationHandler();
        v.validate(doc);
        return v;
    }

}
