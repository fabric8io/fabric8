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
package io.fabric8.bridge.zk.spring;

import io.fabric8.bridge.spring.BridgeConnectorParser;
import io.fabric8.bridge.spring.GatewayConnectorParser;
import io.fabric8.bridge.zk.ZkBridgeConnector;
import io.fabric8.bridge.zk.ZkGatewayConnector;
import io.fabric8.bridge.zk.model.ZkBridgeDestinationsConfigFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Dhiraj Bokde
 *
 */
public class ZkBridgeNamespaceHandler extends NamespaceHandlerSupport {

    private static final String ZKBRIDGE_NS = "http://fusesource.org/fabric/bridge/zookeeper";
    private static final String FABRIC_SERVICE_ATTRIBUTE = "fabricServiceRef";
    private static final String FABRIC_SERVICE_PROPERTY = "fabricService";
    private static final String ZOOKEEPER_ATTRIBUTE = "zooKeeperRef";
    private static final String ZOOKEEPER_PROPERTY = "zooKeeper";
    private static final String EXPORTED_BROKER_PROPERTY = "exportedBrokerConfig";
    private static final String EXPORTED_BROKER_ELEMENT = "exported-broker";

    /* (non-Javadoc)
      * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
      */
	@Override
	public void init() {
		// connectors
        registerBeanDefinitionParser("zkbridge-connector", new ZkBridgeConnectorParser());
        registerBeanDefinitionParser("zkgateway-connector", new ZkGatewayConnectorParser());
        registerBeanDefinitionParser("zkbridge-destinations", new ZkBridgeDestinationsConfigParser());

	}

    private class ZkBridgeConnectorParser extends BridgeConnectorParser {

        @Override
        protected Class getBeanClass(Element element) {
            return ZkBridgeConnector.class;
        }

        @Override
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            // parse base class elements and attributes
            super.doParse(element, parserContext, builder);

            // parse exported-broker element
            NodeList nodes = element.getElementsByTagNameNS(ZKBRIDGE_NS, EXPORTED_BROKER_ELEMENT);
            if (nodes != null && nodes.getLength() == 1) {
                Element exportedBroker = (Element) nodes.item(0);
                builder.addPropertyValue(EXPORTED_BROKER_PROPERTY, brokerConfigParser.parse(element, parserContext));
            }

            // gatewayProfileName is set by base class AbstractSimpleBeanDefinitionParser

            // convert ZooKeeperRef to ZooKeeper bean reference
            String zooKeeperRef = element.getAttribute(ZOOKEEPER_ATTRIBUTE);
            if (StringUtils.hasText(zooKeeperRef)) {
                builder.addPropertyReference(ZOOKEEPER_PROPERTY, zooKeeperRef);
            }

            // convert FabricServiceRef to FabricService bean reference
            String fabricServiceRef = element.getAttribute(FABRIC_SERVICE_ATTRIBUTE);
            if (StringUtils.hasText(fabricServiceRef)) {
                builder.addPropertyReference(FABRIC_SERVICE_PROPERTY, fabricServiceRef);
            }

        }

    }

    private class ZkGatewayConnectorParser extends GatewayConnectorParser {

        @Override
        protected Class getBeanClass(Element element) {
            return ZkGatewayConnector.class;
        }

        @Override
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            // parse base class elements and attributes
            super.doParse(element, parserContext, builder);

            // parse exported-broker element
            NodeList nodes = element.getElementsByTagNameNS(ZKBRIDGE_NS, EXPORTED_BROKER_ELEMENT);
            if (nodes != null && nodes.getLength() == 1) {
                Element exportedBroker = (Element) nodes.item(0);
                builder.addPropertyValue(EXPORTED_BROKER_PROPERTY, brokerConfigParser.parse(element, parserContext));
            }

            // convert ZooKeeperRef to ZooKeeper bean reference
            String zooKeeperRef = element.getAttribute(ZOOKEEPER_ATTRIBUTE);
            if (StringUtils.hasText(zooKeeperRef)) {
                builder.addPropertyReference(ZOOKEEPER_PROPERTY, zooKeeperRef);
            }

            // convert FabricServiceRef to FabricService bean reference
            String fabricServiceRef = element.getAttribute(FABRIC_SERVICE_ATTRIBUTE);
            if (StringUtils.hasText(fabricServiceRef)) {
                builder.addPropertyReference(FABRIC_SERVICE_PROPERTY, fabricServiceRef);
            }

        }

    }

    private class ZkBridgeDestinationsConfigParser extends AbstractSimpleBeanDefinitionParser {

        @Override
        protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
            super.doParse(element, parserContext, builder);

            // set the id as a property, shouldn't this be done by the base class??
            String id = element.getAttribute(ID_ATTRIBUTE);
            if (StringUtils.hasText(id)) {
                builder.addPropertyValue(ID_ATTRIBUTE, id);
            }

            // convert FabricServiceRef to FabricService bean reference
            String fabricServiceRef = element.getAttribute(FABRIC_SERVICE_ATTRIBUTE);
            if (StringUtils.hasText(fabricServiceRef)) {
                builder.addPropertyReference(FABRIC_SERVICE_PROPERTY, fabricServiceRef);
            }

        }

        @Override
        protected Class getBeanClass(Element element) {
            return ZkBridgeDestinationsConfigFactory.class;
        }

    }

}