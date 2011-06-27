/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.internal;

import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.service.FabricServiceImpl;

public class VersionImpl implements Version {

    private final String name;
    private final FabricServiceImpl service;

    public VersionImpl(String name, FabricServiceImpl service) {
        this.name = name;
        this.service = service;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Version getDerivedFrom() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Profile[] getProfiles() {
        return service.getProfiles(name);
    }

    @Override
    public Profile createProfile(String name) {
        return service.createProfile(this.name, name);
    }

    @Override
    public void delete() {
        service.deleteVersion(name);
    }

}
