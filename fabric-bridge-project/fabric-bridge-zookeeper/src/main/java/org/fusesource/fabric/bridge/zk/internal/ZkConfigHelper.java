/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.Stat;
import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.bridge.model.BrokerConfig;
import org.fusesource.fabric.bridge.model.RemoteBridge;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.linkedin.zookeeper.client.IZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.util.StringUtils;

/**
 * @author Dhiraj Bokde
 *
 */
public abstract class ZkConfigHelper {
	
	private static final Logger LOG = LoggerFactory.getLogger(ZkConfigHelper.class);
	private static final String GATEWAY_CONNECTOR_PID = "org.fusesource.fabric.bridge.gatewayRemoteBridge.xml";
	private static final String BRIDGE_CONNECTOR_PID = "org.fusesource.fabric.bridge.bridgeRemoteBridge.xml";
	
	private static JAXBContext jaxbContext;
	
	static {
		try {
			jaxbContext = JAXBContext.newInstance(RemoteBridge.class);
		} catch (JAXBException e) {
			LOG.error("Error creating JAXBContext " + e.getMessage(), e);
		}
	}

	public static RemoteBridge getBridgeConfig(IZKClient client, Agent agent, ApplicationContext context) {
        final String bridgeConfigPath = getBridgeConfigPath(agent);
        RemoteBridge remoteBridge = getData(client, bridgeConfigPath, RemoteBridge.class);
        if (remoteBridge != null) {
            resolveBeanReferences(remoteBridge.getRemoteBrokerConfig(), context);
        }
        return remoteBridge;
    }

    public static void registerBridge(IZKClient client, Agent agent, RemoteBridge remoteBridge) {
        // get data to save
        byte[] data = getZkData(remoteBridge);
        final String bridgeConfigPath = getBridgeConfigPath(agent);
        try {
            Stat stat = client.exists(bridgeConfigPath);
            if (stat == null) {
                client.createBytesNode(bridgeConfigPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            } else {
                client.setByteData(bridgeConfigPath, data);
            }
        } catch (InterruptedException e) {
            String msg = "Error registering bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        } catch (KeeperException e) {
            String msg = "Error registering bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    public static void removeBridge(IZKClient client, Agent agent) {
        final String bridgeConfigPath = getBridgeConfigPath(agent);
        try {
            client.deleteWithChildren(bridgeConfigPath);
        } catch (InterruptedException e) {
            String msg = "Error removing bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        } catch (KeeperException e) {
            String msg = "Error removing bridge config at " + bridgeConfigPath + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        }
    }

    private static String getBridgeConfigPath(Agent agent) {
        return ZkPath.AGENT.getPath(agent.getId()) + "/" + BRIDGE_CONNECTOR_PID;
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

    private static <T> T getData(IZKClient client, String path, Class<T> returnType) {
        try {
            if (client.exists(path) == null) {
                return null;
            }
            byte[] data = client.getData(path);
            // covert data to RemoteBridge
            return getJaxElementFromData(data, returnType);
        } catch (InterruptedException e) {
            String msg = "Error getting " + returnType.getName() + " from " + path + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalArgumentException(msg, e);
        } catch (KeeperException e) {
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
