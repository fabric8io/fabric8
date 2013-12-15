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

import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * A deployment listener that listens for fabric bundles
 * and creates bundles for these.
 */
public class FabDeploymentListener implements ArtifactUrlTransformer {

    private final Logger logger = LoggerFactory.getLogger(FabDeploymentListener.class);

    private DocumentBuilderFactory dbf;
    private boolean deployNonBundles = true;

    public boolean canHandle(File artifact) {
        try {
            String path = artifact.getPath();
            if (path.endsWith(".fab")) {
                return true;
            }
            if (!path.endsWith(".jar")) {
                return false;
            }
            JarFile jar = new JarFile(artifact);
            try {
                Manifest manifest = jar.getManifest();
                boolean answer = false;
                boolean bundle = false;
                if (manifest != null) {
                    Attributes attributes = manifest.getMainAttributes();
                    bundle = isBundle(attributes);
                    for (String name : ServiceConstants.FAB_PROPERTY_NAMES) {
                        if (attributes.getValue(name) != null) {
                            answer = true;
                            break;
                        }
                    }
                }
                if (!answer && isDeployNonBundles()) {
                    answer = !bundle;
                    if (answer) {
                        logger.info("Interpreting the non-bundle jar as a FAB: " + artifact);
                    }
                }
                // TODO filter out if we can find the pom.xml / properties files
                // so that we can get PomDetails.isValid()?
                return answer;
            } finally {
                jar.close();
            }
        } catch (Exception e) {
            logger.error("Unable to parse deployed file " + artifact.getAbsolutePath(), e);
        }
        return false;
    }

    /**
     * Detect if the given attributes denote a valid OSGi bundle
     * @param attributes
     * @return
     */
    protected boolean isBundle(Attributes attributes) {
        return attributes.getValue(ServiceConstants.INSTR_BUNDLE_SYMBOLIC_NAME) != null;
    }

    public URL transform(URL artifact) {
        try {
            return new URL("fab", null, artifact.toString());
        } catch (Exception e) {
            logger.error("Unable to build blueprint application bundle", e);
            return null;
        }
    }

    public boolean isDeployNonBundles() {
        return deployNonBundles;
    }

    public void setDeployNonBundles(boolean deployNonBundles) {
        this.deployNonBundles = deployNonBundles;
    }
}
