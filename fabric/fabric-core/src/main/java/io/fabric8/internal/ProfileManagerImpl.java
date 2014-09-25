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
package io.fabric8.internal;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.permit.PermitManager;
import io.fabric8.api.permit.PermitManager.Permit;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;

import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileManager.class)
public final class ProfileManagerImpl extends AbstractComponent implements ProfileManager {

    @Reference(referenceInterface = PermitManager.class)
    private final ValidatingReference<PermitManager> permitManager = new ValidatingReference<PermitManager>();

    @Activate
    void activate(Map<String, ?> config) {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public Version createVersion(Version version) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.createVersion(version);
        } finally {
            permit.release();
        }
    }

    @Override
    public Version createVersionFrom(String sourceId, String targetId, Map<String, String> attributes) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.createVersionFrom(sourceId, targetId, attributes);
        } finally {
            permit.release();
        }
    }

    @Override
    public List<String> getVersions() {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getVersions();
        } finally {
            permit.release();
        }
    }

    @Override
    public boolean hasVersion(String versionId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.hasVersion(versionId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Version getVersion(String versionId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getVersion(versionId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Version getRequiredVersion(String versionId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getRequiredVersion(versionId);
        } finally {
            permit.release();
        }
    }

    @Override
    public void deleteVersion(String versionId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            service.deleteVersion(versionId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile createProfile(Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.createProfile(profile);
        } finally {
            permit.release();
        }
    }

    @Override
    public boolean hasProfile(String versionId, String profileId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.hasProfile(versionId, profileId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getProfile(String versionId, String profileId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getProfile(versionId, profileId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getRequiredProfile(String versionId, String profileId) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getRequiredProfile(versionId, profileId);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile getOverlayProfile(Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.getOverlayProfile(profile);
        } finally {
            permit.release();
        }
    }

    @Override
    public Profile updateProfile(Profile profile) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            return service.updateProfile(profile);
        } finally {
            permit.release();
        }
    }

    @Override
    public void deleteProfile(String versionId, String profileId, boolean force) {
        Permit<ProfileService> permit = permitManager.get().aquirePermit(ProfileService.PERMIT, false);
        try {
            ProfileService service = permit.getInstance();
            service.deleteProfile(versionId, profileId, force);
        } finally {
            permit.release();
        }
    }

    void bindPermitManager(PermitManager service) {
        this.permitManager.bind(service);
    }

    void unbindPermitManager(PermitManager service) {
        this.permitManager.unbind(service);
    }
}
