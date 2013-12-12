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
package io.fabric8.bridge.zk.internal;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.bridge.model.BrokerConfig;
import io.fabric8.bridge.model.RemoteBridge;
import io.fabric8.zookeeper.ZkPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

/**
 * @author Dhiraj Bokde
 *
 */
public abstract class ZkConfigHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(ZkConfigHelper.class);
	private static final String GATEWAY_CONNECTOR_PID = "io.fabric8.bridge.gatewayRemoteBridge.xml";
	private static final String BRIDGE_CONNECTOR_PID = "io.fabric8.bridge.bridgeRemoteBridge.xml";
	
	private static JAXBContext jaxbContext;
	
	static {
		try {
			jaxbContext = JAXBContext.newInstance(RemoteBridge.class);
		} catch (JAXBException e) {
			LOG.error("Error creating JAXBContext " + e.getMessage(), e);
		}
	}

	public static RemoteBridge getBridgeConfig(CuratorFramework client, Container container, ApplicationContext context) {
        final String bridgeConfigPath = getBridgeConfigPath(container);
        RemoteBridge remoteBridge = getData(client, bridgeConfigPath, RemoteBridge.class);
        if (remoteBridge != null) {
            resolveBeanReferences(remoteBridge.getRemoteBrokerConfig(), context);
        }
        return remoteBridge;
    }

    public static void registerBridge(CuratorFramework client, Container container, RemoteBridge remoteBridge) {
        // get data to save
        byte[] data = getZkData(remoteBridge);
        final String bridgeConfigPath = getBridgeConfigPath(container);
        try {
            setData(client, bridgeConfigPath, data);
        } catch (Exception e) {
            String msg = "Error registering bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static void removeBridge(CuratorFramework client, Container container) {
        final String bridgeConfigPath = getBridgeConfigPath(container);
        try {
            deleteSafe(client, bridgeConfigPath);
        } catch (Exception e) {
            String msg = "Error removing bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static String getBridgeConfigPath(Container container) {
        return ZkPath.CONTAINER.getPath(container.getId()) + "/" + BRIDGE_CONNECTOR_PID;
    }

    public static void registerGateway(Profile gatewayProfile,
            RemoteBridge remoteBridge) {
        Map<String, byte[]> fileConfigurations = gatewayProfile.getFileConfigurations();
        fileConfigurations.put(GATEWAY_CONNECTOR_PID, getZkData(remoteBridge));
        gatewayProfile.setFileConfigurations(fileConfigurations);
    }

    public static RemoteBridge getGatewayConfig(Profile gatewayProfile, ApplicationContext context) {
        byte[] data = gatewayProfile.getFileConfigurations().get(GATEWAY_CONNECTOR_PID);
        RemoteBridge remoteBridge = null;
        if (data != null) {
            remoteBridge = getJaxElementFromData(data, RemoteBridge.class);
            resolveBeanReferences(remoteBridge.getRemoteBrokerConfig(), context);
        }
        return remoteBridge;
    }

    private static void resolveBeanReferences(BrokerConfig remoteBrokerConfig, ApplicationContext context) {
        // connection factory is serialized using jaxb
        // resolve destination resolver if the ref is set, otherwise use default
        final String destinationResolverRef = remoteBrokerConfig.getDestinationResolverRef();
        if (StringUtils.hasText(destinationResolverRef)) {
            remoteBrokerConfig.setDestinationResolver(context.getBean(destinationResolverRef, DestinationResolver.class));
        }
    }

    private static byte[] getZkData(Object jaxbElement) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            jaxbContext.createMarshaller().marshal(jaxbElement, stream);
        } catch (JAXBException e) {
            String msg = "Error marshaling [" + jaxbElement.toString() + "] : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
        return stream.toByteArray();
    }

    private static <T> T getData(CuratorFramework client, String path, Class<T> returnType) {
        try {
            if (exists(client, path) == null) {
                return null;
            }
            byte[] data = client.getData().forPath(path);
            // covert data to RemoteBridge
            return getJaxElementFromData(data, returnType);
        } catch (Exception e) {
            String msg = "Error getting " + returnType.getName() + " from " + path + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static <T> T getJaxElementFromData(byte[] data, Class<T> returnType) {
        if (data == null) {
            return null;
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        try {
            Object retVal = jaxbContext.createUnmarshaller().unmarshal(stream);
            if (returnType.isInstance(retVal)) {
                return returnType.cast(retVal);
            } else {
                String msg = "Expected object of type " + returnType.getName() + ", instead found " + retVal.getClass();
                LOG.error(msg);
                throw new IllegalArgumentException(msg);
            }
        } catch (JAXBException e) {
            String msg = "Error getting " + returnType.getName() + " from fabric data : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

}
