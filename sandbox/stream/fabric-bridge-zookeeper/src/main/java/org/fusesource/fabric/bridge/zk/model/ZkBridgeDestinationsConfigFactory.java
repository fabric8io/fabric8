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
package io.fabric8.bridge.zk.model;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.bridge.model.BridgeDestinationsConfig;
import io.fabric8.bridge.model.BridgedDestination;
import io.fabric8.bridge.model.DispatchPolicy;
import io.fabric8.bridge.model.IdentifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * @author Dhiraj Bokde
 */
@XmlRootElement(name = "zkbridge-destinations")
@XmlAccessorType(XmlAccessType.NONE)
public class ZkBridgeDestinationsConfigFactory
    extends IdentifiedType
    implements FactoryBean<BridgeDestinationsConfig>, InitializingBean {

    private static final String BRIDGE_DESTINATIONS_PID = "io.fabric8.bridge.bridgeDestinationsConfig";
    private static final String DISPATCH_POLICY_PID = "io.fabric8.bridge.dispatchPolicy";
    private static final String PID_XML_EXTENSION = ".xml";
    private static JAXBContext jaxbContext;

    private static final Logger LOG = LoggerFactory.getLogger(ZkBridgeDestinationsConfigFactory.class);
    private static final String NAME_SUFFIX = ".name";

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

        // get current container
        Container container = fabricService.getContainer(System.getProperty("karaf.name"));

        // find destination config with name in associated profiles
        String bridgeDestinationsXml = BRIDGE_DESTINATIONS_PID + "." + getId() + PID_XML_EXTENSION;
        String bridgeDestinationsProperties = BRIDGE_DESTINATIONS_PID + "." + getId();

        List<Profile> profiles = new ArrayList<Profile>();
        for (Profile profile : container.getProfiles()) {

            if (profile.getParents().length > 0) {
                profile = profile.getOverlay();
            }
            profiles.add(profile);

        }

        for (Profile profile : profiles) {

            // look for XML config
            Map<String, byte[]> configs = profile.getFileConfigurations();
            byte[] bytes = configs.get(bridgeDestinationsXml);
            if (bytes != null) {

                Object object = jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes));
                if (object instanceof BridgeDestinationsConfig) {
                    return (BridgeDestinationsConfig) object;
                } else {
                    String msg = "Object at " +
                        bridgeDestinationsXml + " is not of type " +
                        BridgeDestinationsConfig.class.getName() +
                        ", but instead of type " + object.getClass().getName();
                    LOG.error(msg);
                    throw new IllegalArgumentException(msg);
                }

            } else {
                // look for Properties config
                Map<String,String> properties = profile.getConfiguration(bridgeDestinationsProperties);
                if (properties != null) {
                    try {

                        return getBridgeDestinationsProperties(profiles, properties);

                    } catch (BeansException e) {
                        String msg = "Error parsing config properties " + bridgeDestinationsProperties + ": " + e.getMessage();
                        LOG.error(msg, e);
                        throw new BeanCreationException(msg, e);
                    }
                }
            }
        }

        String msg = "No configuration " + bridgeDestinationsXml + " or " +
            bridgeDestinationsProperties + " found in container profiles";
        LOG.error(msg);
        throw new BeanCreationException(msg);
    }

    private BridgeDestinationsConfig getBridgeDestinationsProperties(List<Profile> profiles, Map<String, String> properties)
        throws BeansException {

        // parse bridge config properties
        BridgeDestinationsConfig config = new BridgeDestinationsConfig();
        properties.put("id", getId());

        // first find destination names
        Set<String> prefixSet = new HashSet<String>();
        for (String key : properties.keySet()) {
            if (key.endsWith(NAME_SUFFIX)) {
                String prefix = key.substring(0, key.length() - NAME_SUFFIX.length() + 1);
                if (prefix.matches("^[0-9]+\\.")) {
                    prefixSet.add(prefix);
                } else {
                    String msg = "Invalid destination name " + key + ", must be of the form [0-9]+\\.name";
                    LOG.error(msg);
                    throw new BeanCreationException(msg);
                }
            }
        }

        // extract config properties
        Map<String, String> configProperties = new HashMap<String, String>();
        for (String key : properties.keySet()) {
            boolean skip = false;
            for (String prefix : prefixSet) {
                if (key.startsWith(prefix)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                configProperties.put(key, properties.get(key));
            }
        }

        ConfigurablePropertyAccessor accessor = PropertyAccessorFactory.forDirectFieldAccess(config);
        accessor.setPropertyValues(configProperties);

        Map<String, DispatchPolicy> dispatchPolicies = new HashMap<String, DispatchPolicy>();
        config.setDestinations(getDestinations(properties, prefixSet, getId(), profiles, dispatchPolicies));

        // find and set dispatch policy
        String dispatchPolicyRef = config.getDispatchPolicyRef();
        if (StringUtils.hasText(dispatchPolicyRef)) {
            config.setDispatchPolicy(getDispatchPolicy(dispatchPolicies, profiles,
                dispatchPolicyRef, getId() + "." + dispatchPolicyRef));
        }

        return config;
    }

    private DispatchPolicy getDispatchPolicy(Map<String, DispatchPolicy> dispatchPolicies,
                                             List<Profile> profiles, String dispatchPolicyRef, String id) {

        String dispatchPolicyConfig = DISPATCH_POLICY_PID + "." + dispatchPolicyRef;
        // has the named policy been parsed already??
        if (dispatchPolicies.get(dispatchPolicyConfig) != null) {
            return dispatchPolicies.get(dispatchPolicyConfig);
        }

        for (Profile profile : profiles) {

            Map<String, String> properties = profile.getConfiguration(dispatchPolicyConfig);

            if (properties != null) {
                DispatchPolicy dispatchPolicy = new DispatchPolicy();
                dispatchPolicy.setId(id);

                ConfigurablePropertyAccessor accessor = PropertyAccessorFactory.forDirectFieldAccess(dispatchPolicy);
                accessor.setPropertyValues(properties);

                // add it to the cache
                dispatchPolicies.put(dispatchPolicyConfig, dispatchPolicy);
                return dispatchPolicy;
            }

        }

        String msg = "No configuration " + dispatchPolicyConfig + " found in container profiles";
        LOG.error(msg);
        throw new BeanCreationException(msg);
    }

    private List<BridgedDestination> getDestinations(Map<String, String> properties,
                                                     Set<String> prefixSet, String id, List<Profile> profiles,
                                                     Map<String, DispatchPolicy> dispatchPolicies) {

        List<BridgedDestination> destinations = new ArrayList<BridgedDestination>();
        for (String prefix : prefixSet) {
            // extract destination properties
            Map<String, String> destinationProps = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                 if (entry.getKey().startsWith(prefix)) {
                     destinationProps.put(entry.getKey().substring(prefix.length()), entry.getValue());
                 }
            }

            BridgedDestination destination = new BridgedDestination();
            String destinationId = id + "." + prefix.substring(0, prefix.length() - 1);
            destination.setId(destinationId);

            ConfigurablePropertyAccessor accessor = PropertyAccessorFactory.forDirectFieldAccess(destination);
            accessor.setPropertyValues(destinationProps);

            String dispatchPolicyRef = destination.getDispatchPolicyRef();
            if (StringUtils.hasText(dispatchPolicyRef)) {
                destination.setDispatchPolicy(
                    getDispatchPolicy(dispatchPolicies, profiles,
                        dispatchPolicyRef, destinationId + "." + dispatchPolicyRef));
            }

            destinations.add(destination);
        }

        return destinations;
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
