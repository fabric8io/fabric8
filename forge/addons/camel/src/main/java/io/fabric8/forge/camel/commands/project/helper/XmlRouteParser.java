/**
 *  Copyright 2005-2015 Red Hat, Inc.
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
package io.fabric8.forge.camel.commands.project.helper;

import java.io.InputStream;
import java.util.List;

import io.fabric8.forge.addon.utils.XmlLineNumberParser;
import io.fabric8.forge.camel.commands.project.model.CamelEndpointDetails;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static io.fabric8.forge.camel.commands.project.helper.CamelCatalogHelper.endpointComponentName;
import static io.fabric8.forge.camel.commands.project.helper.CamelXmlHelper.getSafeAttribute;

public class XmlRouteParser {

    public static void parseXmlRoute(InputStream xml, String baseDir, String fullyQualifiedFileName,
                                     List<CamelEndpointDetails> endpoints) throws Exception {

        // find all the endpoints (currently only <endpoint> and within <route>)
        // try parse it as dom
        Document dom = XmlLineNumberParser.parseXml(xml);
        if (dom != null) {
            List<Node> nodes = CamelXmlHelper.findAllEndpoints(dom);
            for (Node node : nodes) {
                String uri = getSafeAttribute(node, "uri");
                String id = getSafeAttribute(node, "id");
                String lineNumber = (String) node.getUserData(XmlLineNumberParser.LINE_NUMBER);

                // we only want the relative dir name from the resource directory, eg META-INF/spring/foo.xml
                String fileName = fullyQualifiedFileName;
                if (fileName.startsWith(baseDir)) {
                    fileName = fileName.substring(baseDir.length() + 1);
                }

                boolean consumerOnly = false;
                boolean producerOnly = false;
                String nodeName = node.getNodeName();
                if ("from".equals(nodeName) || "pollEnrich".equals(nodeName)) {
                    consumerOnly = true;
                } else if ("to".equals(nodeName) || "enrich".equals(nodeName) || "wireTap".equals(nodeName)) {
                    producerOnly = true;
                }

                CamelEndpointDetails detail = new CamelEndpointDetails();
                detail.setFileName(fileName);
                detail.setLineNumber(lineNumber);
                detail.setEndpointInstance(id);
                detail.setEndpointUri(uri);
                detail.setEndpointComponentName(endpointComponentName(uri));
                detail.setConsumerOnly(consumerOnly);
                detail.setProducerOnly(producerOnly);
                endpoints.add(detail);
            }
        }
    }

}
