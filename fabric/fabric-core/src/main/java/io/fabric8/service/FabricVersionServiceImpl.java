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
package io.fabric8.service;

import java.io.InputStream;
import java.util.Properties;

import io.fabric8.api.FabricVersionService;
import io.fabric8.common.util.IOHelpers;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

@Component(name = "io.fabric8.version", immediate = true)
@Service(FabricVersionService.class)
public class FabricVersionServiceImpl implements FabricVersionService {

    private String version;

    public synchronized String getVersion() {
        if (version != null) {
            return version;
        }
        InputStream is = null;
        // try to load from maven properties first
        try {
            Properties p = new Properties();
            is = getClass().getResourceAsStream("/META-INF/maven/io.fabric8/fabric-core/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        } finally {
            if (is != null) {
                IOHelpers.close(is);
            }
        }

        // fallback to using Java API
        if (version == null) {
            Package aPackage = getClass().getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return version;
    }

}
