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
package io.fabric8.fab.osgi;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;

import io.fabric8.fab.DependencyTree;
import io.fabric8.fab.PomDetails;

/**
 * Information about a resolved Fuse Application Bundle.  This class will allow you to access the FAB's InputStream
 * as well as get the list of the additional bundles and features that are required by this FAB.
 */
public interface FabBundleInfo {

    /**
     * Get the original jar url
     */
    String getUrl();

    /**
     * Access the FAB's input stream
     */
    InputStream getInputStream() throws Exception;

    /**
     * Get the computed manifest attributes
     */
    Attributes getManifest();

    /**
     * Get the list of imports determined by the FAB resolver process
     */
    Set<String> getImports();

    /**
     * Get the list of additional required dependencies and bundles
     */
    Collection<DependencyTree> getBundles();

    /**
     * Get the list of additional feature URLs to install the required features for this FAB
     */
    Collection<URI> getFeatureURLs();

    /**
     * Get the list of additional features to be installed for this FAB
     */
    Collection<String> getFeatures();

    /**
     * Get the POM details for the artifact that we resolved as a FAB
     */
    PomDetails getPomDetails();

}
