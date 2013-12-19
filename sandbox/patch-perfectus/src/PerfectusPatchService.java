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
package org.fusesource.fabric.service;

import java.util.Map;
import java.util.Set;

import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;

public interface PerfectusPatchService {

    /**
     * Return the groupId:artifactId:version corresponding to a given url.
     * The url can be a complex url where the maven artifact is embedded, such as when
     * using wrap or war url handlers.
     *
     * @param url the url to get the maven artifact for
     * @return the maven artifact or <code>null</code> if the url isn't maven based
     */
    String getMavenArtifact(String url);

    /**
     * Computes the list of possible upgrades
     *
     * @return a map where keys are maven artifacts associated to a list of possible version upgrades.
     */
    Map<String, Set<String>> getPossibleUpgrades();

    /**
     * Computes the list of possible upgrades for a given Version
     *
     * @return a map where keys are maven artifacts associated to a list of possible version upgrades.
     */
    Map<String, Set<String>> getPossibleUpgrades(Version version);

    /**
     * Computes the list of possible upgrades for a given Profile
     *
     * @return a map where keys are maven artifacts associated to a list of possible version upgrades.
     */
    Map<String, Set<String>> getPossibleUpgrades(Profile profile);

    /**
     * Apply the given upgrades.
     * The upgrades should be a map indexed by artifacts and associated to the new version to use.
     *
     * @param upgrades
     */
    void applyUpgrades(Map<String, String> upgrades);

    /**
     * Apply the given upgrades.
     * The upgrades should be a map indexed by artifacts and associated to the new version to use.
     *
     * @param upgrades
     */
    void applyUpgrades(Version version, Map<String, String> upgrades);

    /**
     * Apply the given upgrades.
     * The upgrades should be a map indexed by artifacts and associated to the new version to use.
     *
     * @param upgrades
     */
    void applyUpgrades(Profile profile, Map<String, String> upgrades);

    /**
     * Load perfectus patches.
     *
     * @param reload force checking remote repositories (by default, results are cached 24 hours).
     * @return a list of available patches
     */
    Set<Patch> loadPerfectusPatches(boolean reload);

    /**
     * Get the set of applicable patches for all fabric Versions.
     * @return
     */
    Set<Patch> getPossiblePatches();

    /**
     * Get the set of applicable patches for a given fabric Version.
     * @param version
     * @return
     */
    Set<Patch> getPossiblePatches(Version version);

    /**
     * Get the set of applicable patches for a given fabric Profile.
     * @param profile
     * @return
     */
    Set<Patch> getPossiblePatches(Profile profile);

    /**
     * Apply patches to all fabric Versions.
     * @param patches
     */
    void applyPatches(Set<Patch> patches);

    /**
     * Apply patches to a given fabric Version.
     * @param version
     * @param patches
     */
    void applyPatches(Version version, Set<Patch> patches);

    /**
     * Apply patches to a given fabric Profile.
     * @param profile
     * @param patches
     */
    void applyPatches(Profile profile, Set<Patch> patches);

}
