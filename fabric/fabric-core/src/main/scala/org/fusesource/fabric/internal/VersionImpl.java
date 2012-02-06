/*
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
package org.fusesource.fabric.internal;

import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.api.VersionSequence;
import org.fusesource.fabric.service.FabricServiceImpl;

import java.util.Arrays;

public class VersionImpl implements Version {

    private final String name;
    private final FabricServiceImpl service;
    private final VersionSequence sequence;

    public VersionImpl(String name, FabricServiceImpl service) {
        this.name = name;
        this.service = service;
        this.sequence = new VersionSequence(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public VersionSequence getSequence() {
        return sequence;
    }

    @Override
    public int compareTo(Version that) {
        return this.sequence.compareTo(that.getSequence());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionImpl version = (VersionImpl) o;

        if (name != null ? !name.equals(version.name) : version.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public Version getDerivedFrom() {
        // TODO how to find the derived from???
        return null;
    }

    @Override
    public Profile[] getProfiles() {
        return service.getProfiles(name);
    }

    @Override
    public Profile getProfile(String name) {
        return service.getProfile(this.name, name);
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
