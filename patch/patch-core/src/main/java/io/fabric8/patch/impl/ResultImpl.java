/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.patch.impl;

import java.util.Collection;

import io.fabric8.patch.Patch;
import io.fabric8.patch.Result;
import io.fabric8.patch.BundleUpdate;

public class ResultImpl implements Result {

    private final Patch patch;
    private final boolean simulation;
    private final long date;
    private final Collection<BundleUpdate> updates;
    private final String startup;
    private final String overrides;

    public ResultImpl(Patch patch,
                      boolean simulation,
                      long date,
                      Collection<BundleUpdate> updates,
                      String startup,
                      String overrides) {
        this.patch = patch;
        this.simulation = simulation;
        this.date = date;
        this.updates = updates;
        this.startup = startup;
        this.overrides = overrides;
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

    public String getStartup() {
        return startup;
    }

    public String getOverrides() {
        return overrides;
    }
}
