/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import org.fusesource.fabric.api.Agent;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.fusesource.fabric.internal.ProfileImpl.*;

public class ProfileOverlayImpl implements Profile {

    private final ProfileImpl self;
    private Map<String, Map<String, String>> rc;

    public ProfileOverlayImpl(ProfileImpl self) {
        this.self = self;
    }

    @Override
    public String getId() {
        return self.getId();
    }

    @Override
    public String getVersion() {
        return self.getVersion();
    }

    @Override
    public Profile[] getParents() {
        return self.getParents();
    }

    public List<String> getBundles() {
        return getAgentConfigList(this, ConfigListType.BUNDLES);
    }

    public List<String> getFeatures() {
        return getAgentConfigList(this, ConfigListType.FEATURES);
    }

    public List<String> getRepositories() {
        return getAgentConfigList(this, ConfigListType.REPOSITORIES);
    }

    @Override
    public Agent[] getAssociatedAgents() {
        return self.getAssociatedAgents();
    }

    @Override
    public void setFileConfigurations(Map<String, byte[]> configurations) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setParents(Profile[] parents) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    @Override
    public void setConfigurations(Map<String, Map<String, String>> configurations) {
        throw new UnsupportedOperationException("Overlay profiles are read-only.");
    }

    public void delete() {
        throw new UnsupportedOperationException("Can not delete an overlay profile");
    }

    @Override
    public Profile getOverlay() {
        return this;
    }

    @Override
    public boolean isOverlay() {
        return true;
    }

    static private class SupplementControl {
        byte [] data;
        Properties props;
    }

    @Override
    public Map<String, byte[]> getFileConfigurations() {
        try {
            Map<String, SupplementControl> aggregate = new HashMap<String, SupplementControl>();
            supplement(this, aggregate);

            Map<String, byte[]> rc = new HashMap<String, byte[]>();
            for (Map.Entry<String, SupplementControl> entry : aggregate.entrySet()) {
                SupplementControl ctrl = entry.getValue();
                if( ctrl.props!=null ) {
                    ctrl.data = toBytes(ctrl.props);
                }
                rc.put(entry.getKey(), ctrl.data);
            }
            return rc;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

    private void supplement(Profile profile, Map<String, SupplementControl> aggregate) throws Exception {
        for (Profile p : profile.getParents()) {
            supplement(p, aggregate);
        }

        Map<String, byte[]> configs = profile.getFileConfigurations();
        for (Map.Entry<String, byte[]> entry : configs.entrySet()) {
            // we can use fine grained inheritance based updating if it's
            // a properties file.
            String fileName = entry.getKey();
            if( fileName.endsWith(".properties") ) {
                SupplementControl ctrl = aggregate.get(fileName);
                if( ctrl!=null ) {
                    // we can update the file..

                    Properties childMap = toProperties(entry.getValue());
                    if( childMap.remove(DELETED)!=null ) {
                        ctrl.props.clear();
                    }

                    // Update the entries...
                    for (Map.Entry<Object, Object> p: childMap.entrySet()){
                        if( DELETED.equals(p.getValue()) ) {
                            ctrl.props.remove(p.getKey());
                        } else {
                            ctrl.props.put(p.getKey(), p.getValue());
                        }
                    }

                } else {
                    // new file..
                    ctrl.props = toProperties(entry.getValue());
                    aggregate.put(fileName, ctrl);
                }
            } else {
                // not a properties file? we can only overwrite.
                SupplementControl ctrl = new SupplementControl();
                ctrl.data = entry.getValue();
                aggregate.put(fileName, ctrl);
            }
        }
    }

    @Override
    public Map<String, Map<String, String>> getConfigurations() {
        try {
            Map<String, SupplementControl> aggregate = new HashMap<String, SupplementControl>();
            supplement(this, aggregate);

            Map<String, Map<String, String>> rc = new HashMap<String, Map<String, String>>();
            for (Map.Entry<String, SupplementControl> entry : aggregate.entrySet()) {
                SupplementControl ctrl = entry.getValue();
                if( ctrl.props!=null ) {
                    rc.put(stripSuffix(entry.getKey(), ".properties"), toMap(ctrl.props));
                }
            }
            return rc;
        } catch (Exception e) {
            throw new FabricException(e);
        }
    }

}
