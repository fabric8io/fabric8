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

import org.fusesource.patch.BundleUpdate;
import org.fusesource.patch.Patch;
import org.fusesource.patch.Result;

public class ResultImpl implements Result {

    private final Patch patch;
    private final boolean simulation;
    private final long date;
    private final Collection<BundleUpdate> updates;

    public ResultImpl(Patch patch, boolean simulation, long date, Collection<BundleUpdate> updates) {
        this.patch = patch;
        this.simulation = simulation;
        this.date = date;
        this.updates = updates;
    }

    public Patch getPatch() {
        return patch;
    }

    public boolean isSimulation() {
        return simulation;
    }

    public long getDate() {
        return date;
    }

    public Collection<BundleUpdate> getUpdates() {
        return updates;
    }

}
