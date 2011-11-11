/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk.model;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.model.IdentifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;

/**
 * @author Dhiraj Bokde
 */
@XmlRootElement(name = "zkbridge-destinations")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkBridgeDestinationsConfigFactory
    extends IdentifiedType
    implements FactoryBean<BridgeDestinationsConfig>, InitializingBean {

    private static final String BRIDGE_DESTINATIONS_PID = "org.fusesource.fabric.bridge.bridgeDestinationsConfig";
    private static final String PID_EXTENSION = ".xml";
    private static JAXBContext jaxbContext;

    private static final Logger LOG = LoggerFactory.getLogger(ZkBridgeDestinationsConfigFactory.class);

    static {
        try {
            jaxbContext = JAXBContext.newInstance(BridgeDestinationsConfig.class);
        } catch (JAXBException e) {
            String msg = "Error creating JAXB context for " + BridgeDestinationsConfig.class + " : " + e.getMessage();
            LOG.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

    private FabricService fabricService;

    @XmlAttribute(required = true)
    private String fabricServiceRef;

    @Override
    public BridgeDestinationsConfig getObject() throws Exception {

        // get current agent
        Agent agent = fabricService.getAgent(System.getProperty("karaf.name"));

        // find destination config with name in associated profiles
        String bridgeDestinationsConfigName = BRIDGE_DESTINATIONS_PID + "." + getId() + PID_EXTENSION;

        for (Profile profile : agent.getProfiles()) {

            if (profile.getParents().length > 0) {
                profile = profile.getOverlay();
            }

            byte[] bytes = profile.getFileConfigurations().get(bridgeDestinationsConfigName);
            if (bytes != null) {

                Object object = jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes));
                if (object instanceof BridgeDestinationsConfig) {
                    return (BridgeDestinationsConfig) object;
                } else {
                    String msg = "Object at " +
                        bridgeDestinationsConfigName + " is not of type " +
                        BridgeDestinationsConfig.class.getName() +
                        ", but instead of type " + object.getClass().getName();
                    LOG.error(msg);
                    throw new IllegalArgumentException(msg);
                }

            }
        }

        String msg = "No configuration " + bridgeDestinationsConfigName + " found in agent profiles";
        LOG.error(msg);
        throw new BeanCreationException(msg);
    }

    @Override
    public Class<?> getObjectType() {
        return BridgeDestinationsConfig.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getId() == null) {
            throw new IllegalArgumentException("Property name must be set");
        }
        if (getFabricService() == null) {
            throw new IllegalArgumentException("Property fabricService must be set");
        }
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public String getFabricServiceRef() {
        return fabricServiceRef;
    }

    public void setFabricServiceRef(String fabricServiceRef) {
        this.fabricServiceRef = fabricServiceRef;
    }

}
