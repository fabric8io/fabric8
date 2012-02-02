/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.patch.impl;

import java.util.Collection;

import org.fusesource.patch.Patch;
import org.fusesource.patch.Result;

public class PatchImpl implements Patch {

    private final ServiceImpl service;
    private final String id;
    private final String description;
    private final Collection<String> bundles;
    private Result result;

    public PatchImpl(ServiceImpl service, String id, String description, Collection<String> bundles) {
        this.service = service;
        this.id = id;
        this.description = description;
        this.bundles = bundles;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Collection<String> getBundles() {
        return bundles;
    }

    public boolean isInstalled() {
        return result != null;
    }

    @Override
    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @Override
    public Result simulate() {
        return service.install(this, true);
    }

    @Override
    public Result install() {
        return service.install(this, false);
    }

    @Override
    public void rollback(boolean force) {
        service.rollback(this, force);
    }

}
